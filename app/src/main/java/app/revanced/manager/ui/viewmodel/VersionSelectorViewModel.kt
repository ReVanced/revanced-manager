package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import app.revanced.manager.util.PM
import app.revanced.manager.util.patchCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class VersionSelectorViewModel(
    val input: SelectedAppInfo.VersionSelector.ViewModelParams
) : ViewModel(), KoinComponent {
    private val patchBundleRepository: PatchBundleRepository = get()
    private val downloadedAppsRepository: DownloadedAppRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val pm: PM = get()

    val patchCount = input.patchSelection.patchCount

    val downloadedVersions = downloadedAppsRepository.get(input.packageName)
        .map { apps ->
            apps.map { it.version }
        }

    private val _localVersion = MutableStateFlow<String?>(null)
    val localVersion: StateFlow<String?> = _localVersion

    val availableVersions = combine(
        patchBundleRepository.suggestedVersions(input.packageName, input.patchSelection),
        _localVersion,
    ) { versions, local ->
        versions.orEmpty()
            .let { versions ->
                local?.let {
                    versions.toMutableMap().also { it.putIfAbsent(local, 0) }
                } ?: versions
            }
            .map { (key, value) -> SelectedVersion.Specific(key) to patchCount - value }
            .sortedWith(
                compareBy<Pair<SelectedVersion.Specific, Int>>{ it.second }
                    .thenByDescending { it.first.version }
            )
    }

    var installedAppVersion by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val currentApp = pm.getPackageInfo(input.packageName)
            val patchedApp = installedAppRepository.get(input.packageName)

            if (patchedApp?.installType == InstallType.DEFAULT) return@launch

            installedAppVersion = currentApp?.versionName
        }
        input.localPath?.let { local ->
            viewModelScope.launch {
                val packageInfo = pm.getPackageInfo(File(local))
                _localVersion.value = packageInfo?.versionName
            }
        }
    }

    var selectedVersion by mutableStateOf(input.selectedVersion)
        private set

    fun selectVersion(version: SelectedVersion) {
        selectedVersion = version
    }
}