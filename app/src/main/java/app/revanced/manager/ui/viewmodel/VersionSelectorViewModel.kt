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

    var selectedVersion by mutableStateOf(input.selectedVersion)
        private set

    var installedAppVersion by mutableStateOf<String?>(null)
        private set

    fun selectVersion(version: SelectedVersion) {
        selectedVersion = version
    }

    private val _localVersion = MutableStateFlow<String?>(null)
    val localVersion: StateFlow<String?> = _localVersion

    val downloadedVersions = downloadedAppsRepository.get(input.packageName)
        .map { apps -> apps.map { it.version } }

    val availableVersions = combine(
        patchBundleRepository.suggestedVersions(input.packageName, input.patchSelection),
        _localVersion,
    ) { versions, local ->
        val allVersions = versions.orEmpty().toMutableMap().apply {
            if (local != null) putIfAbsent(local, 0)
        }

        allVersions
            .map { (version, supported) -> SelectedVersion.Specific(version) to patchCount - supported }
            .sortedWith(compareBy<Pair<SelectedVersion.Specific, Int>> { it.second }.thenByDescending { it.first.version })
    }

    init {
        viewModelScope.launch {
            val patchedApp = installedAppRepository.get(input.packageName)
            if (patchedApp?.installType != InstallType.DEFAULT) {
                installedAppVersion = pm.getPackageInfo(input.packageName)?.versionName
            }

            input.localPath?.let { local ->
                _localVersion.value = pm.getPackageInfo(File(local))?.versionName
            }
        }
    }
}