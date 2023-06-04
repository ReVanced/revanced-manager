package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.util.AppInfo
import app.revanced.manager.util.PatchesSelection
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Stable
class PatchesSelectorViewModel(appInfo: AppInfo) : ViewModel(), KoinComponent {
    val bundlesFlow = get<SourceRepository>().bundles.map { bundles ->
        bundles.mapValues { (_, bundle) -> bundle.patches }.map { (name, patches) ->
            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()

            patches.filter { it.compatibleWith(appInfo.packageName) }.forEach {
                val targetList = if (it.supportsVersion(appInfo.packageInfo!!.versionName)) supported else unsupported

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