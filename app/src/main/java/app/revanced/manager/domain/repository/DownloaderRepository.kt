package app.revanced.manager.domain.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import android.util.Log
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.downloader.TrustedDownloader
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.downloader.DownloaderPackageState
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.downloader.DownloaderBuilder
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.Scope
import app.revanced.manager.util.PM
import app.revanced.manager.util.tag
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.lang.reflect.Modifier

@OptIn(DownloaderHostApi::class)
class DownloaderRepository(
    private val pm: PM,
    private val prefs: PreferencesManager,
    private val app: Application,
    db: AppDatabase
) {
    private val trustDao = db.trustedDownloaderDao()
    private val _downloaderPackageStates =
        MutableStateFlow(emptyMap<String, DownloaderPackageState>())
    val downloaderPackageStates = _downloaderPackageStates.asStateFlow()
    val loadedDownloadersFlow = downloaderPackageStates.map { states ->
        states.values.filterIsInstance<DownloaderPackageState.Loaded>().flatMap { it.downloader }
    }

    private val acknowledgedPackageNames = prefs.acknowledgedDownloaders
    private val installedDownloaderPackageNames = MutableStateFlow(emptySet<String>())
    val newDownloaderPackageNames = combine(
        installedDownloaderPackageNames,
        acknowledgedPackageNames.flow
    ) { installed, acknowledged ->
        installed subtract acknowledged
    }

    suspend fun reload() {
        val downloaderPackages =
            withContext(Dispatchers.IO) {
                pm.getPackagesWithFeature(DOWNLOADER_FEATURE)
                    .associate { it.packageName to loadPackage(it.packageName) }
            }

        _downloaderPackageStates.value = downloaderPackages
        installedDownloaderPackageNames.value = downloaderPackages.keys

        val acknowledgedDownloader = this@DownloaderRepository.acknowledgedPackageNames.get()
        val uninstalledDownloader =
            acknowledgedDownloader subtract installedDownloaderPackageNames.value
        if (uninstalledDownloader.isNotEmpty()) {
            Log.d(tag, "Uninstalled downloader: ${uninstalledDownloader.joinToString(", ")}")
            this@DownloaderRepository.acknowledgedPackageNames.update(acknowledgedDownloader subtract uninstalledDownloader)
            trustDao.removeAll(uninstalledDownloader)
        }
    }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        val state =
            (_downloaderPackageStates.value[data.downloaderPackageName] as? DownloaderPackageState.Loaded)
                ?: throw Exception("Downloader package name ${data.downloaderPackageName} is not available")
        val downloader = state.downloader.firstOrNull { it.className == data.downloaderClassName }
            ?: throw Exception("No downloader with name ${data.downloaderClassName} found in ${data.downloaderPackageName}")

        return downloader to data.unwrapWith(state.classLoader)
    }

    private suspend fun loadPackage(packageName: String): DownloaderPackageState {
        try {
            if (!verify(packageName)) return DownloaderPackageState.Untrusted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while verifying downloader $packageName", e)
            return DownloaderPackageState.Failed(e)
        }

        return try {
            val packageInfo = pm.getPackageInfo(packageName, flags = PackageManager.GET_META_DATA)!!
            val classNames =
                packageInfo.applicationInfo!!.metaData.getStringArray(METADATA_DOWNLOADER_CLASSES)
                    ?: throw Exception("Missing metadata attribute $METADATA_DOWNLOADER_CLASSES")

            val classLoader =
                PathClassLoader(packageInfo.applicationInfo!!.sourceDir, app.classLoader)
            val downloaderContext = app.createPackageContext(packageName, 0)

            val scopeImpl = object : Scope {
                override val hostPackageName = app.packageName
                override val downloaderPackageName = downloaderContext.packageName
            }

            DownloaderPackageState.Loaded(
                classNames.map { className ->
                    val builder = classLoader
                        .loadClass(className)
                        .getDownloaderBuilder()
                    val downloader = builder.build(
                        scopeImpl = scopeImpl,
                        context = downloaderContext
                    )

                    LoadedDownloader(
                        packageName,
                        className,
                        downloaderContext.getString(builder.name),
                        packageInfo.versionName!!,
                        downloader.get,
                        downloader.download
                    )
                },
                classLoader,
                with(pm) { packageInfo.label() }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(tag, "Failed to load downloader $packageName", t)
            DownloaderPackageState.Failed(t)
        }
    }

    suspend fun trustPackage(packageName: String) {
        trustDao.upsertTrust(
            TrustedDownloader(
                packageName,
                pm.getSignature(packageName).toByteArray()
            )
        )

        reload()
        prefs.edit {
            acknowledgedPackageNames += packageName
        }
    }

    suspend fun revokeTrustForPackage(packageName: String) =
        trustDao.remove(packageName).also { reload() }

    suspend fun acknowledgeAll() =
        acknowledgedPackageNames.update(installedDownloaderPackageNames.value)

    private suspend fun verify(packageName: String): Boolean {
        val expectedSignature =
            trustDao.getTrustedSignature(packageName) ?: return false

        return pm.hasSignature(packageName, expectedSignature)
    }

    private companion object {
        const val DOWNLOADER_FEATURE = "app.revanced.manager.downloader"
        const val METADATA_DOWNLOADER_CLASSES = "app.revanced.manager.downloader.classes"

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