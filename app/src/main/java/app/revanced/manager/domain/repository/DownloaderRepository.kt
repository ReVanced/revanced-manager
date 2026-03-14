package app.revanced.manager.domain.repository

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.downloader.TrustedDownloader
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.downloader.DownloaderPackageState
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.downloader.DownloaderBuilder
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.Scope
import app.revanced.manager.util.PM
import app.revanced.manager.util.tag
import dalvik.system.PathClassLoader
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.io.File
import java.lang.reflect.Modifier

@OptIn(DownloaderHostApi::class)
class DownloaderRepository(
    private val pm: PM,
    private val prefs: PreferencesManager,
    private val app: Application,
    private val reVancedAPI: ReVancedAPI,
    private val http: HttpService,
    private val ackpineInstaller: PackageInstaller,
    db: AppDatabase
) {
    sealed interface ApiDownloaderActionResult {
        data class Success(val packageName: String) : ApiDownloaderActionResult
        data object NoAsset : ApiDownloaderActionResult
        data object NoInstalled : ApiDownloaderActionResult
        data object NotTargetDownloader : ApiDownloaderActionResult
        data object NoUpdate : ApiDownloaderActionResult
        data object Aborted : ApiDownloaderActionResult
        data object Failed : ApiDownloaderActionResult
    }

    private val trustDao = db.trustedDownloaderDao()
    private val _downloaderPackageStates =
        MutableStateFlow(emptyMap<String, DownloaderPackageState>())
    val downloaderPackageStates = _downloaderPackageStates.asStateFlow()
    val apiDownloaderPackageName = prefs.apiDownloaderPackage.flow.map { it.takeIf(String::isNotEmpty) }
    val loadedDownloadersFlow = downloaderPackageStates.map { states ->
        states.values.filterIsInstance<DownloaderPackageState.Loaded>().flatMap { it.downloaders }
    }

    private val acknowledgedPackageNames = prefs.acknowledgedDownloaders
    private val installedDownloaderPackageNames = MutableStateFlow(emptySet<String>())
    val newDownloaderPackageNames = combine(
        installedDownloaderPackageNames,
        acknowledgedPackageNames.flow
    ) { installed, acknowledged ->
        installed subtract acknowledged
    }

    private val downloadersDir = app.getDir("downloaders", Context.MODE_PRIVATE)

    suspend fun reload() {
        val downloaderPackages =
            withContext(Dispatchers.IO) {
                pm.getPackagesWithFeature(DOWNLOADER_FEATURE)
                    .associate { it.packageName to loadPackage(it) }
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

            withContext(Dispatchers.IO) {
                uninstalledDownloader.forEach { downloadersDir.resolve(it).deleteRecursively() }
            }

            val apiPkg = prefs.apiDownloaderPackage.get()
            if (apiPkg in uninstalledDownloader) {
                prefs.apiDownloaderPackage.update("")
            }
        }
    }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        val state =
            (_downloaderPackageStates.value[data.downloaderPackageName] as? DownloaderPackageState.Loaded)
                ?: throw Exception("Downloader package name ${data.downloaderPackageName} is not available")
        val downloader = state.downloaders.firstOrNull { it.className == data.downloaderClassName }
            ?: throw Exception("No downloader with name ${data.downloaderClassName} found in ${data.downloaderPackageName}")

        return downloader to data.unwrapWith(state.classLoader)
    }

    private suspend fun loadPackage(packageInfo: PackageInfo): DownloaderPackageState {
        val packageName = packageInfo.packageName
        try {
            if (!verify(packageName)) return DownloaderPackageState.Untrusted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while verifying downloader $packageName", e)
            return DownloaderPackageState.Failed(e)
        }

        return try {
            val downloaderContext = app.createPackageContext(packageName, 0)

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
                    downloadersDir.resolve(downloaderPackageName).also(File::mkdirs)
            }

            DownloaderPackageState.Loaded(
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
                        packageInfo.versionName!!,
                        scopeImpl,
                        downloader
                    )
                },
                classLoader,
                downloaderContext,
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

    suspend fun getApiDownloaderAsset(): ReVancedAsset? = try {
        reVancedAPI.getDownloaderAsset().getOrThrow()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch API downloader info", e)
        null
    }

    suspend fun getInstalledApiDownloader(): Pair<String, String>? {
        val packageName = prefs.apiDownloaderPackage.get().takeIf { it.isNotEmpty() } ?: return null
        val version = pm.getPackageInfo(packageName)?.versionName ?: return null
        return packageName to version
    }

    suspend fun checkApiDownloaderUpdate(): ReVancedAsset? {
        val asset = getApiDownloaderAsset() ?: return null
        val installed = getInstalledApiDownloader()
        if (installed != null) {
            val installedVersion = installed.second.removePrefix("v")
            val remoteVersion = asset.version.removePrefix("v")
            if (installedVersion == remoteVersion) return null
        }
        return asset
    }

    suspend fun installLatestApiDownloader(
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
        onInstalling: ((Boolean) -> Unit)? = null,
    ): ApiDownloaderActionResult {
        val asset = getApiDownloaderAsset() ?: return ApiDownloaderActionResult.NoAsset
        return performApiDownloaderInstall(asset, onProgress, onInstalling)
    }

    suspend fun updateInstalledApiDownloader(
        packageName: String? = null,
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
        onInstalling: ((Boolean) -> Unit)? = null,
    ): ApiDownloaderActionResult {
        val installed = getInstalledApiDownloader() ?: return ApiDownloaderActionResult.NoInstalled
        if (packageName != null && installed.first != packageName) {
            return ApiDownloaderActionResult.NotTargetDownloader
        }

        val asset = checkApiDownloaderUpdate() ?: return ApiDownloaderActionResult.NoUpdate
        return performApiDownloaderInstall(asset, onProgress, onInstalling)
    }

    suspend fun installApiDownloaderAsset(
        asset: ReVancedAsset,
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
        onInstalling: ((Boolean) -> Unit)? = null,
    ): ApiDownloaderActionResult = performApiDownloaderInstall(asset, onProgress, onInstalling)

    private suspend fun performApiDownloaderInstall(
        asset: ReVancedAsset,
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
        onInstalling: ((Boolean) -> Unit)? = null,
    ): ApiDownloaderActionResult = withContext(Dispatchers.IO) {
        getInstalledApiDownloader()?.let { (packageName, installedVersion) ->
            if (installedVersion.removePrefix("v") == asset.version.removePrefix("v")) {
                Log.i(tag, "API downloader $packageName is already up to date (${asset.version})")
                return@withContext ApiDownloaderActionResult.Success(packageName)
            }
        }

        val tempFile = File.createTempFile("api_downloader", ".apk", app.cacheDir)
        try {
            http.download(tempFile) {
                url(asset.downloadUrl)
                onDownload { bytesSentTotal, contentLength ->
                    onProgress(bytesSentTotal, contentLength)
                }
            }

            val apkSignature = pm.getApkSignature(tempFile)
            val managerSignature = pm.getManagerSignature()
            val signatureMatches = apkSignature != null && apkSignature == managerSignature

            val pkgInfo = pm.getPackageInfo(tempFile)
                ?: throw Exception("Could not parse downloaded APK")
            val packageName = pkgInfo.packageName

            onInstalling?.invoke(true)
            val result = ackpineInstaller.createSession(Uri.fromFile(tempFile)) {
                confirmation = Confirmation.IMMEDIATE
            }.await()

            when (result) {
                is Session.State.Failed<*> -> {
                    val failure = result.failure as? InstallFailure
                    if (failure is InstallFailure.Aborted) {
                        return@withContext ApiDownloaderActionResult.Aborted
                    }

                    Log.e(tag, "Failed to install API downloader: ${result.failure}")
                    return@withContext ApiDownloaderActionResult.Failed
                }
                Session.State.Succeeded -> {
                    Log.i(tag, "Successfully installed API downloader: $packageName")
                }
            }
            onInstalling?.invoke(false)

            prefs.apiDownloaderPackage.update(packageName)

            if (signatureMatches) {
                trustPackage(packageName)
            } else {
                reload()
            }

            ApiDownloaderActionResult.Success(packageName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Failed to download/install API downloader", e)
            ApiDownloaderActionResult.Failed
        } finally {
            tempFile.delete()
        }
    }

    private companion object {
        const val DOWNLOADER_FEATURE = "app.revanced.manager.downloader"
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