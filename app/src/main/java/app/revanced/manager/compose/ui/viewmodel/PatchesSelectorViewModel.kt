package app.revanced.manager.compose.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import app.revanced.manager.compose.domain.repository.BundleRepository
import app.revanced.manager.compose.patcher.patch.PatchInfo
import app.revanced.manager.compose.util.PackageInfo
import app.revanced.manager.compose.util.PatchesSelection
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class PatchesSelectorViewModel(packageInfo: PackageInfo) : ViewModel(), KoinComponent {
    val bundlesFlow = get<BundleRepository>().bundles.map { bundles ->
        bundles.mapValues { (_, bundle) -> bundle.patches }.map { (name, patches) ->
            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()

            patches.filter { it.compatibleWith(packageInfo.packageName) }.forEach {
                val targetList = if (it.supportsVersion(packageInfo.packageName)) supported else unsupported

                targetList.add(it)
            }

            Bundle(name, supported, unsupported)
        }
    }

    private val selectedPatches = mutableStateListOf<Pair<String, String>>()
    fun isSelected(bundle: String, patch: PatchInfo) = selectedPatches.contains(bundle to patch.name)
    fun togglePatch(bundle: String, patch: PatchInfo) {
        val pair = bundle to patch.name
        if (isSelected(bundle, patch)) selectedPatches.remove(pair) else selectedPatches.add(pair)
    }

    fun generateSelection(): PatchesSelection = HashMap<String, MutableList<String>>().apply {
        selectedPatches.forEach { (bundleName, patchName) ->
            this.getOrPut(bundleName, ::mutableListOf).add(patchName)
        }
    }

    data class Bundle(
        val name: String, val supported: List<PatchInfo>, val unsupported: List<PatchInfo>
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