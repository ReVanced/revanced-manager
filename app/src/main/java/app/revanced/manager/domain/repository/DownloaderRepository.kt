package app.revanced.manager.domain.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import android.util.Log
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.downloader.TrustedDownloader
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.downloader.DownloaderState
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
    private val _downloaderStates = MutableStateFlow(emptyMap<String, DownloaderState>())
    val downloaderStates = _downloaderStates.asStateFlow()
    val loadedDownloaderFlow = downloaderStates.map { states ->
        states.values.filterIsInstance<DownloaderState.Loaded>().map { it.downloader }
    }

    private val acknowledgedDownloader = prefs.acknowledgedDownloader
    private val installedDownloaderPackageNames = MutableStateFlow(emptySet<String>())
    val newDownloaderPackageNames = combine(
        installedDownloaderPackageNames,
        acknowledgedDownloader.flow
    ) { installed, acknowledged ->
        installed subtract acknowledged
    }

    suspend fun reload() {
        val downloader =
            withContext(Dispatchers.IO) {
                pm.getPackagesWithFeature(DOWNLOADER_FEATURE)
                    .associate { it.packageName to loadDownloader(it.packageName) }
            }

        _downloaderStates.value = downloader
        installedDownloaderPackageNames.value = downloader.keys

        val acknowledgedDownloader = this@DownloaderRepository.acknowledgedDownloader.get()
        val uninstalledDownloader = acknowledgedDownloader subtract installedDownloaderPackageNames.value
        if (uninstalledDownloader.isNotEmpty()) {
            Log.d(tag, "Uninstalled downloader: ${uninstalledDownloader.joinToString(", ")}")
            this@DownloaderRepository.acknowledgedDownloader.update(acknowledgedDownloader subtract uninstalledDownloader)
            trustDao.removeAll(uninstalledDownloader)
        }
    }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        val downloader =
            (_downloaderStates.value[data.downloaderPackageName] as? DownloaderState.Loaded)?.downloader
                ?: throw Exception("Downloader with name ${data.downloaderPackageName} is not available")

        return downloader to data.unwrapWith(downloader)
    }

    private suspend fun loadDownloader(packageName: String): DownloaderState {
        try {
            if (!verify(packageName)) return DownloaderState.Untrusted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while verifying downloader $packageName", e)
            return DownloaderState.Failed(e)
        }

        return try {
            val packageInfo = pm.getPackageInfo(packageName, flags = PackageManager.GET_META_DATA)!!
            val className = packageInfo.applicationInfo!!.metaData.getString(METADATA_DOWNLOADER_CLASS)
                ?: throw Exception("Missing metadata attribute $METADATA_DOWNLOADER_CLASS")

            val classLoader =
                PathClassLoader(packageInfo.applicationInfo!!.sourceDir, app.classLoader)
            val downloaderContext = app.createPackageContext(packageName, 0)

            val downloader = classLoader
                .loadClass(className)
                .getDownloaderBuilder()
                .build(
                    scopeImpl = object : Scope {
                        override val hostPackageName = app.packageName
                        override val downloaderPackageName = downloaderContext.packageName
                    },
                    context = downloaderContext
                )

            DownloaderState.Loaded(
                LoadedDownloader(
                    packageName,
                    with(pm) { packageInfo.label() },
                    packageInfo.versionName!!,
                    downloader.get,
                    downloader.download,
                    classLoader
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(tag, "Failed to load downloader $packageName", t)
            DownloaderState.Failed(t)
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
            acknowledgedDownloader += packageName
        }
    }

    suspend fun revokeTrustForPackage(packageName: String) =
        trustDao.remove(packageName).also { reload() }

    suspend fun acknowledgeAllNewDownloader() =
        acknowledgedDownloader.update(installedDownloaderPackageNames.value)

    private suspend fun verify(packageName: String): Boolean {
        val expectedSignature =
            trustDao.getTrustedSignature(packageName) ?: return false

        return pm.hasSignature(packageName, expectedSignature)
    }

    private companion object {
        const val DOWNLOADER_FEATURE = "app.revanced.manager.downloader"
        const val METADATA_DOWNLOADER_CLASS = "app.revanced.manager.downloader.class"

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