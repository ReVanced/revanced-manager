package app.revanced.manager.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.network.downloader.DownloaderPackage
import app.revanced.manager.util.PM
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderRepository: DownloaderRepository,
    prefs: PreferencesManager,
    val pm: PM,
    val networkInfo: NetworkInfo,
) : ViewModel() {
    val usePrereleases = prefs.useDownloaderPrerelease
    val downloaderSources = downloaderRepository.downloaderSources
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

    var isUpdatingDownloader by mutableStateOf(false)
        private set

    var deletingDownloaderUid by mutableStateOf<Int?>(null)
        private set

    fun updateUsePrereleases(value: Boolean) = viewModelScope.launch {
        usePrereleases.update(value)

        // Rebuilds the default source with the URL of the new release channel.
        downloaderRepository.reload()
        val apiSource = downloaderRepository.downloaderSources.first()[0] ?: return@launch
        updateDownloader(apiSource)
    }

    fun importSource(downloaderUri: Uri) = viewModelScope.launch {
        downloaderRepository.importFrom(downloaderUri)
    }

    fun createSource(apiUrl: String, autoUpdate: Boolean) = viewModelScope.launch {
        downloaderRepository.create(apiUrl, autoUpdate)
    }

    suspend fun validateSourceUrl(apiUrl: String) = downloaderRepository.validateUrl(apiUrl)

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
        downloaderRepository.updateCheck()
        isRefreshingDownloaders = false
    }

    fun deleteDownloader(src: Source<DownloaderPackage>) = viewModelScope.launch {
        try {
            deletingDownloaderUid = src.uid
            downloaderRepository.remove(src)
        } finally {
            deletingDownloaderUid = null
        }
    }

    fun updateDownloader(src: Source<DownloaderPackage>) = viewModelScope.launch {
        try {
            isUpdatingDownloader = true
            downloaderRepository.update(src, showToast = true)
        } finally {
            isUpdatingDownloader = false
        }
    }

    fun setAutoUpdate(src: Source<DownloaderPackage>, value: Boolean) = viewModelScope.launch {
        with(downloaderRepository) {
            src.setAutoUpdate(value)
        }
    }

    fun setEndpoint(src: Source<DownloaderPackage>, value: String) = viewModelScope.launch {
        val endpoint = value.trim()
        if (src.uri.toString() == endpoint) return@launch

        with(downloaderRepository) {
            src.setEndpoint(endpoint)
        }

        downloaderSources.first()[src.uid]?.let { updated ->
            updateDownloader(updated)
        }
    }
}