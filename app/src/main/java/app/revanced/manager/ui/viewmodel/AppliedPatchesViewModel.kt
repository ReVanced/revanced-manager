package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.installed.InstalledPatchBundle
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.util.PatchSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppliedPatchesViewModel(
    packageName: String
) : ViewModel(), KoinComponent {
    private val installedAppRepository: InstalledAppRepository by inject()

    var appliedPatches: PatchSelection? by mutableStateOf(null)
        private set
    var patchBundles: List<InstalledPatchBundle> by mutableStateOf(emptyList())
        private set

    init {
        viewModelScope.launch {
            appliedPatches = withContext(Dispatchers.IO) {
                installedAppRepository.getAppliedPatches(packageName)
            }
            patchBundles = withContext(Dispatchers.IO) {
                installedAppRepository.getInstalledPatchBundles(packageName)
            }
        }
    }
}
