package app.revanced.manager.domain.repository

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import androidx.annotation.RequiresApi
import app.revanced.manager.R
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.downloader.DownloaderEntity
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.domain.manager.SourceManager
import app.revanced.manager.domain.sources.APISource
import app.revanced.manager.domain.sources.JsonSource
import app.revanced.manager.domain.sources.Loader
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.downloader.DownloaderBuilder
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.Scope
import app.revanced.manager.network.downloader.DownloaderPackage
import app.revanced.manager.util.PM
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import kotlin.time.Instant
import app.revanced.manager.data.room.sources.Source as SourceInfo

@OptIn(DownloaderHostApi::class)
class DownloaderRepository(
    private val pm: PM,
    app: Application,
    db: AppDatabase
) : SourceManager<DownloaderEntity, DownloaderPackage, List<LoadedDownloader>>(
    emptyList(),
    app.getDir("downloaders", Context.MODE_PRIVATE)
) {
    private val dao = db.downloaderDao()

    override suspend fun dbGetAll() = dao.all()
    override suspend fun dbGetProps(uid: Int) = dao.getProps(uid)
    override suspend fun dbUpsert(entity: DownloaderEntity) = dao.upsert(entity)
    override suspend fun dbRemove(uid: Int) = dao.remove(uid)
    override suspend fun dbReset() = dao.reset()

    private val loader = Loader { file ->
        val dataDir = file.parentFile!!.resolve("data").also(File::mkdirs)
        val pkgInfo = pm.getPackageInfo(file) ?: error("Failed to get package info for $file")
        loadPackage(pkgInfo, dataDir)
    }

    override fun loadEntity(entity: DownloaderEntity): Source<DownloaderPackage> = with(entity) {
        val file = directoryOf(uid).resolve("downloader.jar")
        val actualName =
            name.ifEmpty { app.getString(if (uid == 0) R.string.auto_updates_dialog_downloaders else R.string.source_name_fallback) }

        val releasedAt = entity.releasedAt?.let {
            Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        }

        return when (source) {
            is SourceInfo.Local -> LocalSource(actualName, uid, null, file, loader)
            is SourceInfo.API -> APISource(
                actualName,
                uid,
                versionHash,
                releasedAt,
                null,
                file,
                SourceInfo.API.SENTINEL,
                autoUpdate,
                loader
            ) { getDownloaderUpdate() }

            is SourceInfo.Remote -> JsonSource(
                actualName,
                uid,
                versionHash,
                releasedAt,
                null,
                file,
                source.url.toString(),
                autoUpdate,
                loader
            )
        }
    }

    override fun entityFromProps(
        uid: Int,
        props: SourceProperties
    ) = DownloaderEntity(
        uid,
        name = props.name,
        versionHash = props.versionHash,
        source = props.source,
        autoUpdate = props.autoUpdate,
        releasedAt = props.releasedAt
    )

    override fun realNameOf(loaded: DownloaderPackage) = loaded.name

    override val updateFailed = R.string.downloader_update_failed
    override val updateSuccess = R.string.patches_update_success
    override val updateUnavailable = R.string.patches_update_unavailable
    override val replaceFail = R.string.downloader_replace_fail

    override suspend fun loadDataFromSources(sources: MutableMap<Int, Source<DownloaderPackage>>) =
        sources.values.flatMap { src -> src.loaded?.downloaders.orEmpty() }

    val downloaderSources = store.state.map { it.sources }
    val loadedDownloadersFlow = store.state.map { it.data }

    fun findPackageByName(packageName: String) =
        store.state.value.sources.values.asSequence().mapNotNull { it.loaded }
            .find { it.packageName == packageName }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        val pkg = findPackageByName(data.downloaderPackageName)
            ?: throw Exception("Downloader package ${data.downloaderPackageName} is not available")
        val downloader = pkg.downloaders.firstOrNull { it.className == data.downloaderClassName }
            ?: throw Exception("No downloader with name ${data.downloaderClassName} found in ${data.downloaderPackageName}")

        return downloader to data.unwrapWith(pkg.classLoader)
    }

    private fun loadPackage(packageInfo: PackageInfo, dataDir: File): DownloaderPackage {
        val packageName = packageInfo.packageName
        val resources = pm.getResources(packageInfo)

        val classNamesResId =
            @SuppressLint("DiscouragedApi") resources.getIdentifier(
                CLASSES_RESOURCE_NAME,
                "array",
                packageName
            )
        if (classNamesResId == 0) throw Exception("Class names resource not found (array/$CLASSES_RESOURCE_NAME)")
        val classNames = resources.getStringArray(classNamesResId)

        val apkPath = packageInfo.applicationInfo!!.sourceDir
        val classLoader =
            PathClassLoader(apkPath, app.classLoader)

        val scopeImpl = object : Scope {
            override val hostPackageName = app.packageName
            override val downloaderPackageName = packageName
            override val dataDir = dataDir
        }

        val resourceImpl =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Api30ResourceImpl(File(apkPath))
            else OldResourceImpl(resources)

        val packageLabel = with(pm) { packageInfo.label() }

        return DownloaderPackage(
            classNames.map { className ->
                val downloader = classLoader
                    .loadClass(className)
                    .getDownloaderBuilder()
                    .build(
                        scopeImpl = scopeImpl,
                        context = app,
                        resources = resources
                    )

                LoadedDownloader(
                    packageName,
                    packageLabel,
                    className,
                    resources.getString(downloader.name),
                    scopeImpl,
                    downloader
                )
            },
            classLoader,
            resourceImpl,
            packageInfo.packageName,
            packageLabel,
            packageInfo.versionName.orEmpty()
        )
    }

    /**
     * Provides resources for [app.revanced.manager.DownloaderActivity]. Has a better implementation on Android 11 and above.
     */
    fun interface ResourceImpl {
        fun apply(res: Resources): Resources?
    }

    private class OldResourceImpl(val resources: Resources) : ResourceImpl {
        @Suppress("DEPRECATION")
        override fun apply(res: Resources) =
            resources.also { it.updateConfiguration(res.configuration, res.displayMetrics) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private class Api30ResourceImpl(private val file: File) : ResourceImpl {
        private var weakRef: WeakReference<ResourcesLoader>? = null

        private fun getLoader(): ResourcesLoader {
            weakRef?.get()?.let { return it }

            val provider =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    ResourcesProvider.loadFromApk(pfd)
                }
            val loader = ResourcesLoader().apply { addProvider(provider) }
            weakRef = WeakReference(loader)

            return loader
        }

        override fun apply(res: Resources): Resources? {
            val loader = getLoader()
            res.addLoaders(loader)
            return null
        }
    }

    private companion object {
        const val CLASSES_RESOURCE_NAME = "app.revanced.manager.downloader.classes"

        const val PUBLIC_STATIC = Modifier.PUBLIC or Modifier.STATIC
        val Int.isPublicStatic get() = (this and PUBLIC_STATIC) == PUBLIC_STATIC
        val Class<*>.isDownloaderBuilder get() = DownloaderBuilder::class.java.isAssignableFrom(this)

        @Suppress("UNCHECKED_CAST")
        fun Class<*>.getDownloaderBuilder() =
            declaredMethods
                .firstOrNull { it.modifiers.isPublicStatic && it.returnType.isDownloaderBuilder && it.parameterTypes.isEmpty() }
                ?.let { it(null) as DownloaderBuilder<Parcelable> }
                ?: throw Exception("Could not find a valid downloader implementation in class $canonicalName")
    }
}