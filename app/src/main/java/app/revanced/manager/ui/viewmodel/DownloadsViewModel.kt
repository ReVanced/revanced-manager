package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.DownloadedApp
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel(
    private val downloadedAppRepository: DownloadedAppRepository,
    val prefs: PreferencesManager
) : ViewModel() {
    val downloadedApps = downloadedAppRepository.getAll().map { downloadedApps ->
        downloadedApps.sortedWith(
            compareBy<DownloadedApp> {
                it.packageName
            }.thenBy { it.version }
        )
    }

    val selection = mutableStateSetOf<DownloadedApp>()

    fun toggleItem(downloadedApp: DownloadedApp) {
        if (selection.contains(downloadedApp))
            selection.remove(downloadedApp)
        else
            selection.add(downloadedApp)
    }

    fun delete() {
        viewModelScope.launch(NonCancellable) {
            downloadedAppRepository.delete(selection)

            withContext(Dispatchers.Main) {
                selection.clear()
            }
        }
    }

}