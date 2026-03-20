package app.revanced.manager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
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
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.sources.RemoteSource
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
    app: Application,
    private val downloadedAppRepository: DownloadedAppRepository,
    private val downloaderRepository: DownloaderRepository,
    prefs: PreferencesManager,
    val pm: PM,
    val networkInfo: NetworkInfo,
) : ViewModel() {
    private val contentResolver = app.contentResolver
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
        val apiSource = downloaderRepository.downloaderSources.first()[0]?.asRemoteOrNull ?: return@launch
        updateDownloader(apiSource)
    }

    @SuppressLint("Recycle")
    fun createLocalSource(downloaderUri: Uri) = viewModelScope.launch {
        downloaderRepository.createLocal { contentResolver.openInputStream(downloaderUri)!! }
    }

    fun createRemoteSource(apiUrl: String, autoUpdate: Boolean) = viewModelScope.launch {
        downloaderRepository.createRemote(apiUrl, autoUpdate)
    }

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

    fun updateDownloader(src: RemoteSource<DownloaderPackage>) = viewModelScope.launch {
        try {
            isUpdatingDownloader = true
            downloaderRepository.update(src, showToast = true)
        } finally {
            isUpdatingDownloader = false
        }
    }

    fun setAutoUpdate(src: RemoteSource<DownloaderPackage>, value: Boolean) = viewModelScope.launch {
        with(downloaderRepository) {
            src.setAutoUpdate(value)
        }
    }
}