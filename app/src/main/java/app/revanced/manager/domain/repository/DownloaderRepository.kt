package app.revanced.manager.domain.repository

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Parcelable
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
import java.io.File
import java.lang.reflect.Modifier
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

    override val defaultSource = DownloaderEntity(
        uid = 0,
        name = "",
        versionHash = null,
        source = SourceInfo.API,
        autoUpdate = false
    )

    override suspend fun dbGetAll() = dao.all()
    override suspend fun dbGetProps(uid: Int) = dao.getProps(uid)
    override suspend fun dbUpsert(entity: DownloaderEntity) = dao.upsert(entity)
    override suspend fun dbRemove(uid: Int) = dao.remove(uid)
    override suspend fun dbReset() = dao.reset()

    private val loader = Loader { file ->
        val pkgInfo = pm.getPackageInfo(file) ?: error("Failed to get package info for $file")
        loadPackage(pkgInfo)
    }

    override fun loadEntity(entity: DownloaderEntity): Source<DownloaderPackage> = with(entity) {
        val file = directoryOf(uid).resolve("downloader.jar")

        return when (source) {
            is SourceInfo.Local -> LocalSource(name, uid, null, file, loader)
            is SourceInfo.API -> APISource(
                name,
                uid,
                versionHash,
                null,
                file,
                SourceInfo.API.SENTINEL,
                autoUpdate,
                loader
            ) { getDownloaderUpdate() }

            is SourceInfo.Remote -> JsonSource(
                name,
                uid,
                versionHash,
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
        autoUpdate = props.autoUpdate
    )

    override fun uidOf(entity: DownloaderEntity) = entity.uid
    override fun realNameOf(loaded: DownloaderPackage) = loaded.name

    override val updateFailed = R.string.downloader_update_failed
    override val updateSuccess = R.string.patches_update_success
    override val updateUnavailable = R.string.patches_update_unavailable
    override val replaceFail = R.string.downloader_replace_fail

    override suspend fun loadDataFromSources(sources: MutableMap<Int, Source<DownloaderPackage>>) =
        sources.values.flatMap { src -> src.loaded?.downloaders.orEmpty() }

    val downloaderSources = store.state.map { it.sources }
    val loadedDownloadersFlow = store.state.map { it.data }

    // TODO: clear data for removed downloaders.
    private val dataDir = app.getDir("downloaders_data", Context.MODE_PRIVATE)

    fun findPackageByName(packageName: String) =
        store.state.value.sources.values.asSequence().mapNotNull { it.loaded }
            .find { it.context.packageName == packageName }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        val pkg = findPackageByName(data.downloaderPackageName) ?: throw Exception("Downloader package ${data.downloaderPackageName} is not available")
        val downloader = pkg.downloaders.firstOrNull { it.className == data.downloaderClassName }
            ?: throw Exception("No downloader with name ${data.downloaderClassName} found in ${data.downloaderPackageName}")

        return downloader to data.unwrapWith(pkg.classLoader)
    }

    private val createApplicationContext by lazy {
        val clazz = Context::class.java
        clazz.getMethod("createApplicationContext", ApplicationInfo::class.java, Int::class.java)
    }

    private fun loadPackage(packageInfo: PackageInfo): DownloaderPackage {
        val packageName = packageInfo.packageName

        // The context is technically only necessary for resources. On API levels 30 and above, it would be better to use the proper APIs for dynamic resource loading.
        val downloaderContext = createApplicationContext(
            app,
            packageInfo.applicationInfo,
            0
        ) as Context

        val classNamesResId =
            @SuppressLint("DiscouragedApi") downloaderContext.resources.getIdentifier(
                CLASSES_RESOURCE_NAME,
                "array",
                downloaderContext.packageName
            )
        if (classNamesResId == 0) throw Exception("Class names resource not found (array/$CLASSES_RESOURCE_NAME)")
        val classNames = downloaderContext.resources.getStringArray(classNamesResId)

        val classLoader =
            PathClassLoader(packageInfo.applicationInfo!!.sourceDir, app.classLoader)

        val scopeImpl = object : Scope {
            override val hostPackageName = app.packageName
            override val downloaderPackageName = downloaderContext.packageName
            override val dataDir =
                this@DownloaderRepository.dataDir.resolve(downloaderPackageName).also(File::mkdirs)
        }

        return DownloaderPackage(
            classNames.map { className ->
                val downloader = classLoader
                    .loadClass(className)
                    .getDownloaderBuilder()
                    .build(
                        scopeImpl = scopeImpl,
                        context = downloaderContext
                    )

                LoadedDownloader(
                    packageName,
                    className,
                    downloaderContext.getString(downloader.name),
                    scopeImpl,
                    downloader
                )
            },
            classLoader,
            downloaderContext,
            with(pm) { packageInfo.label() },
            packageInfo.versionName.orEmpty()
        )
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