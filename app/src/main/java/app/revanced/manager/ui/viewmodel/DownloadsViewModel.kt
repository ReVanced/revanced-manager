package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderPluginRepository: DownloaderPluginRepository,
    val pm: PM
) : ViewModel() {
    val downloaderPluginStates = downloaderPluginRepository.pluginStates
    val downloadedApps = downloadedAppRepository.getAll().map { downloadedApps ->
        downloadedApps.sortedWith(
            compareBy<DownloadedApp> {
                it.packageName
            }.thenBy { it.version }
        )
    }
    val appSelection = mutableStateSetOf<DownloadedApp>()

    var isRefreshingPlugins by mutableStateOf(false)
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

    fun refreshPlugins() = viewModelScope.launch {
        isRefreshingPlugins = true
        downloaderPluginRepository.reload()
        isRefreshingPlugins = false
    }

    fun trustPlugin(packageName: String) = viewModelScope.launch {
        downloaderPluginRepository.trustPackage(packageName)
    }

    fun revokePluginTrust(packageName: String) = viewModelScope.launch {
        downloaderPluginRepository.revokeTrustForPackage(packageName)
    }
}