package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.util.AppInfo
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.SnapshotStateSet
import app.revanced.manager.util.flatMapLatestAndCombine
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.toMutableStateSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Stable
class PatchesSelectorViewModel(
    val appInfo: AppInfo
) : ViewModel(), KoinComponent {
    private val selectionRepository: PatchSelectionRepository = get()
    private val prefs: PreferencesManager = get()
    val allowExperimental get() = prefs.allowExperimental

    val bundlesFlow = get<SourceRepository>().sources.flatMapLatestAndCombine(
        combiner = { it }
    ) { source ->
        // Regenerate bundle information whenever this source updates.
        source.bundle.map { bundle ->
            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()
            val universal = mutableListOf<PatchInfo>()

            bundle.patches.filter { it.compatibleWith(appInfo.packageName) }.forEach {
                val targetList =
                    if (it.compatiblePackages == null) universal else if (it.supportsVersion(
                            appInfo.packageInfo!!.versionName
                        )
                    ) supported else unsupported

                targetList.add(it)
            }

            BundleInfo(source.name, source.uid, bundle.patches, supported, unsupported, universal)
        }
    }

    private val selectedPatches = mutableStateMapOf<Int, SnapshotStateSet<String>>()

    var showOptionsDialog by mutableStateOf(false)
        private set

    val compatibleVersions = mutableStateListOf<String>()

    var filter by mutableStateOf(SHOW_SUPPORTED or SHOW_UNSUPPORTED)
        private set

    private fun getOrCreateSelection(bundle: Int) =
        selectedPatches.getOrPut(bundle, ::mutableStateSetOf)

    fun isSelected(bundle: Int, patch: PatchInfo) =
        selectedPatches[bundle]?.contains(patch.name) ?: false

    fun togglePatch(bundle: Int, patch: PatchInfo) {
        val name = patch.name
        val patches = getOrCreateSelection(bundle)

        if (patches.contains(name)) patches.remove(name) else patches.add(name)
    }

    suspend fun getAndSaveSelection(): PatchesSelection =
        selectedPatches.also {
            withContext(Dispatchers.Default) {
                selectionRepository.updateSelection(appInfo.packageName, it)
            }
        }.mapValues { it.value.toMutableList() }.apply {
            if (allowExperimental) {
                return@apply
            }

            // Filter out unsupported patches that may have gotten selected through the database if the setting is not enabled.
            bundlesFlow.first().forEach {
                this[it.uid]?.removeAll(it.unsupported.map { patch -> patch.name })
            }
        }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val bundles = bundlesFlow.first()
            val filteredSelection =
                selectionRepository.getSelection(appInfo.packageName).mapValues { (uid, patches) ->
                    // Filter out patches that don't exist.
                    val filteredPatches = bundles.singleOrNull { it.uid == uid }
                        ?.let { bundle ->
                            val allPatches = bundle.all.map { it.name }
                            patches.filter { allPatches.contains(it) }
                        }
                        ?: patches

                    filteredPatches.toMutableStateSet()
                }

            withContext(Dispatchers.Main) {
                selectedPatches.putAll(filteredSelection)
            }
        }
    }

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
            patch.compatiblePackages?.find { it.name == appInfo.packageName }
                ?.let { compatiblePackage ->
                    set.addAll(compatiblePackage.versions)
                }
        }

        compatibleVersions.addAll(set)
    }

    fun toggleFlag(flag: Int) {
        filter = filter xor flag
    }

    companion object {
        const val SHOW_SUPPORTED = 1 // 2^0
        const val SHOW_UNIVERSAL = 2 // 2^1
        const val SHOW_UNSUPPORTED = 4 // 2^2
    }

    data class BundleInfo(
        val name: String,
        val uid: Int,
        val all: List<PatchInfo>,
        val supported: List<PatchInfo>,
        val unsupported: List<PatchInfo>,
        val universal: List<PatchInfo>
    )
}