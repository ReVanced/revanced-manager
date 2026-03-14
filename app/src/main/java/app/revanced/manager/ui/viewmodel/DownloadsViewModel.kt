package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.network.downloader.DownloaderPackage
import app.revanced.manager.util.PM
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(
    private val app: Application,
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderRepository: DownloaderRepository,
    prefs: PreferencesManager,
    private val networkInfo: NetworkInfo,
    val pm: PM
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
        // TODO: fix me!
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

    fun updateDownloader(src: RemoteSource<DownloaderPackage>) = viewModelScope.launch {
        try {
            isUpdatingDownloader = true
            downloaderRepository.update(src)
        } finally {
            isUpdatingDownloader = false
        }
    }
}