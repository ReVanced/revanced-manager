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
class PatchesSelectorViewModel(
    val appInfo: AppInfo
) : ViewModel(), KoinComponent {

    val bundlesFlow = get<SourceRepository>().bundles.map { bundles ->
        bundles.mapValues { (_, bundle) -> bundle.patches }.map { (name, patches) ->
            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()
            val universal = mutableListOf<PatchInfo>()

            patches.filter { it.compatibleWith(appInfo.packageName) }.forEach {
                val targetList = if (it.compatiblePackages == null) universal else if (it.supportsVersion(appInfo.packageInfo!!.versionName)) supported else unsupported

                targetList.add(it)
            }

            Bundle(name, supported, unsupported, universal)
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
        val name: String,
        val supported: List<PatchInfo>,
        val unsupported: List<PatchInfo>,
        val universal: List<PatchInfo>
    )

    var showOptionsDialog by mutableStateOf(false)
        private set

    val compatibleVersions = mutableStateListOf<String>()

    fun dismissDialogs() {
        showOptionsDialog = false
        compatibleVersions.clear()
    }

    fun openOptionsDialog() {
        showOptionsDialog = true
    }

    fun openUnsupportedDialog(unsupportedVersions: List<PatchInfo>) {
        val set = HashSet<String>()

        unsupportedVersions.forEach { patch ->
            patch.compatiblePackages?.find { it.name == appInfo.packageName }?.let { compatiblePackage ->
                set.addAll(compatiblePackage.versions)
            }
        }

        compatibleVersions.addAll(set)
    }

    var filter by mutableStateOf(SHOW_SUPPORTED or SHOW_UNSUPPORTED)
        private set

    fun toggleFlag(flag: Int) {
        filter = filter xor flag
    }

    companion object {
        const val SHOW_SUPPORTED = 1 // 2^0
        const val SHOW_UNIVERSAL = 2 // 2^1
        const val SHOW_UNSUPPORTED = 4 // 2^2
    }
}