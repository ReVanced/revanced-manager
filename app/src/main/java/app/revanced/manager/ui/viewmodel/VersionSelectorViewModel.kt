package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import app.revanced.manager.util.patchCount
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class VersionSelectorViewModel(
    val input: SelectedAppInfo.VersionSelector.ViewModelParams
) : ViewModel(), KoinComponent {
    val patchBundleRepository: PatchBundleRepository = get()


    val patchCount = input.patchSelection.patchCount

    val availableVersions = patchBundleRepository.suggestedVersions(input.packageName, input.patchSelection)
        .map { versions ->
            versions.orEmpty()
                .map { (key, value) -> SelectedVersion.Specific(key) to patchCount - value }
                .sortedWith(
                    compareBy<Pair<SelectedVersion.Specific, Int>>{ it.second }
                        .thenByDescending { it.first.version }
                )
        }


    var selectedVersion by mutableStateOf(input.selectedVersion)
        private set

    fun selectVersion(version: SelectedVersion) {
        selectedVersion = version
    }


}