package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderRepository: DownloaderRepository,
    val pm: PM
) : ViewModel() {
    val downloaderStates = downloaderRepository.downloaderPackageStates
    val downloadedApps = downloadedAppRepository.getAll().map { downloadedApps ->
        downloadedApps.sortedWith(
            compareBy<DownloadedApp> {
                it.packageName
            }.thenBy { it.version }
        )
    }
    val appSelection = mutableStateSetOf<DownloadedApp>()

    var isRefreshingDownloader by mutableStateOf(false)
        private set

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

    fun refreshDownloader() = viewModelScope.launch {
        isRefreshingDownloader = true
        downloaderRepository.reload()
        isRefreshingDownloader = false
    }

    fun trustDownloader(packageName: String) = viewModelScope.launch {
        downloaderRepository.trustPackage(packageName)
    }

    fun revokeDownloaderTrust(packageName: String) = viewModelScope.launch {
        downloaderRepository.revokeTrustForPackage(packageName)
    }
}