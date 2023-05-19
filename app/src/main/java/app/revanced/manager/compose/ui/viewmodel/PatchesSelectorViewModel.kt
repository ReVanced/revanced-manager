package app.revanced.manager.compose.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import app.revanced.manager.compose.patcher.data.repository.PatchesRepository
import app.revanced.manager.compose.patcher.patch.PatchInfo
import app.revanced.manager.compose.util.PackageInfo
import kotlinx.coroutines.flow.map

class PatchesSelectorViewModel(packageInfo: PackageInfo, patchesRepository: PatchesRepository) :
    ViewModel() {
    val bundlesFlow = patchesRepository.getPatchInformation().map { patches ->
        val supported = mutableListOf<PatchInfo>()
        val unsupported = mutableListOf<PatchInfo>()

        patches.filter { it.compatibleWith(packageInfo.packageName) }.forEach {
            val targetList = if (it.supportsVersion(packageInfo.packageName)) supported else unsupported

            targetList.add(it)
        }

        listOf(
            Bundle(
                name = "official",
                supported, unsupported
            )
        )
    }

    val selectedPatches = mutableStateListOf<String>()

    fun isSelected(patch: PatchInfo) = selectedPatches.contains(patch.name)
    fun togglePatch(patch: PatchInfo) {
        val name = patch.name
        if (isSelected(patch)) selectedPatches.remove(name) else selectedPatches.add(patch.name)
    }

    data class Bundle(
        val name: String,
        val supported: List<PatchInfo>,
        val unsupported: List<PatchInfo>
    )

    var showOptionsDialog by mutableStateOf(false)
        private set
    var showUnsupportedDialog by mutableStateOf(false)
        private set

    fun dismissDialogs() {
        showOptionsDialog = false
        showUnsupportedDialog = false
    }

    fun openOptionsDialog() {
        showOptionsDialog = true
    }

    fun openUnsupportedDialog() {
        showUnsupportedDialog = true
    }
}