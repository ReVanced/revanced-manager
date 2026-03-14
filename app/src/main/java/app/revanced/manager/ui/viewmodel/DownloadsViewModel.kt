package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure

class DownloadsViewModel(
    private val app: Application,
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderRepository: DownloaderRepository,
    private val networkInfo: NetworkInfo,
    val pm: PM
) : ViewModel() {
    enum class DownloaderInstallState {
        IDLE,
        DOWNLOADING,
        INSTALLING
    }

    val downloaderStates = downloaderRepository.downloaderPackageStates
    val apiDownloaderPackageName = downloaderRepository.apiDownloaderPackageName
    val downloadedApps = downloadedAppRepository.getAll().map { downloadedApps ->
        downloadedApps.sortedWith(
            compareBy<DownloadedApp> {
                it.packageName
            }.thenBy { it.version }
        )
    }
    val appSelection = mutableStateSetOf<DownloadedApp>()

    var isRefreshingDownloaders by mutableStateOf(false)
        private set

    var downloaderInstallState by mutableStateOf(DownloaderInstallState.IDLE)
        private set

    var isUpdatingDownloader by mutableStateOf(false)
        private set

    var deletingDownloaderPackageName by mutableStateOf<String?>(null)
        private set

    private var installDownloaderJob: Job? = null

    fun toggleApp(downloadedApp: DownloadedApp) {
        if (appSelection.contains(downloadedApp))
            appSelection.remove(downloadedApp)
        else
            appSelection.add(downloadedApp)
    }

    fun deleteApps() {
        viewModelScope.launch(NonCancellable) {
            downloadedAppRepository.delete(appSelection)

            withContext(Dispatchers.Main) {
                appSelection.clear()
            }
        }
    }

    fun refreshDownloaders() = viewModelScope.launch {
        isRefreshingDownloaders = true
        downloaderRepository.reload()
        isRefreshingDownloaders = false
    }

    fun trustDownloader(packageName: String) = viewModelScope.launch {
        downloaderRepository.trustPackage(packageName)
    }

    fun revokeDownloaderTrust(packageName: String) = viewModelScope.launch {
        downloaderRepository.revokeTrustForPackage(packageName)
    }

    fun installDefaultDownloader() {
        if (downloaderInstallState != DownloaderInstallState.IDLE) return
        if (installDownloaderJob?.isActive == true) return

        installDownloaderJob = viewModelScope.launch {
            downloaderInstallState = DownloaderInstallState.DOWNLOADING
            try {
                val result = downloaderRepository.installLatestApiDownloader(
                    onInstalling = { installing ->
                        if (installing) {
                            downloaderInstallState = DownloaderInstallState.INSTALLING
                        }
                    }
                )
                when (result) {
                    DownloaderRepository.ApiDownloaderActionResult.Aborted,
                    is DownloaderRepository.ApiDownloaderActionResult.Success -> Unit

                    else -> app.toast(app.getString(R.string.api_downloader_failed))
                }
            } finally {
                downloaderInstallState = DownloaderInstallState.IDLE
                installDownloaderJob = null
            }
        }
    }

    fun cancelDefaultDownloaderInstall() {
        installDownloaderJob?.cancel()
    }

    fun updateDownloader(packageName: String) = viewModelScope.launch {
        if (isUpdatingDownloader) return@launch
        if (!networkInfo.isConnected()) {
            app.toast(app.getString(R.string.no_network_toast))
            return@launch
        }

        isUpdatingDownloader = true
        try {
            when (downloaderRepository.updateInstalledApiDownloader(packageName)) {
                DownloaderRepository.ApiDownloaderActionResult.NoInstalled,
                DownloaderRepository.ApiDownloaderActionResult.NotTargetDownloader,
                DownloaderRepository.ApiDownloaderActionResult.NoUpdate -> {
                    app.toast(app.getString(R.string.no_update_available))
                }

                DownloaderRepository.ApiDownloaderActionResult.Aborted,
                is DownloaderRepository.ApiDownloaderActionResult.Success -> Unit

                DownloaderRepository.ApiDownloaderActionResult.NoAsset,
                DownloaderRepository.ApiDownloaderActionResult.Failed -> {
                    app.toast(app.getString(R.string.api_downloader_failed))
                }
            }
        } finally {
            isUpdatingDownloader = false
        }
    }

    fun deleteDownloader(packageName: String) = viewModelScope.launch {
        if (deletingDownloaderPackageName != null) return@launch

        deletingDownloaderPackageName = packageName
        try {
            when (val result = pm.uninstallPackage(packageName)) {
                is Session.State.Failed<UninstallFailure> -> {
                    if (result.failure !is UninstallFailure.Aborted) {
                        val msg = result.failure.message.orEmpty()
                        app.toast(
                            this@DownloadsViewModel.app.getString(
                                R.string.uninstall_app_fail,
                                msg
                            )
                        )
                    }
                    return@launch
                }

                Session.State.Succeeded -> downloaderRepository.reload()
            }
        } finally {
            if (deletingDownloaderPackageName == packageName) {
                deletingDownloaderPackageName = null
            }
        }
    }
}