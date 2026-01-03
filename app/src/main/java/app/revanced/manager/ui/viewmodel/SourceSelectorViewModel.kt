package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import app.revanced.manager.util.PM
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class SourceSelectorViewModel(
    val input: SelectedAppInfo.SourceSelector.ViewModelParams
) : ViewModel(), KoinComponent {
    private val downloadedAppRepository: DownloadedAppRepository = get()
    private val pm: PM = get()

    val downloadedApps = downloadedAppRepository.get(input.packageName)
        .map { it.sortedByDescending { app -> app.version } }

    var selectedSource by mutableStateOf(input.selectedSource)
        private set

    fun selectSource(source: SelectedSource) {
        selectedSource = source
    }

    fun selectDownloadedApp(app: DownloadedApp) {
        val file = downloadedAppRepository.getApkFileForApp(app)

        selectedSource = SelectedSource.Downloaded(file.path, app.version)
    }

    var installedVersion by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val packageInfo = pm.getPackageInfo(input.packageName)

            installedVersion = packageInfo?.versionName
        }
    }

}