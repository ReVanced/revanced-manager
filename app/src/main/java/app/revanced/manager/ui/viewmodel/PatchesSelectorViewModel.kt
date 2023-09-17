package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.SnapshotStateSet
import app.revanced.manager.util.flatMapLatestAndCombine
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.saver.snapshotStateSetSaver
import app.revanced.manager.util.toMutableStateSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Stable
@OptIn(SavedStateHandleSaveableApi::class)
class PatchesSelectorViewModel(
    val input: Destination.PatchesSelector
) : ViewModel(), KoinComponent {
    private val selectionRepository: PatchSelectionRepository = get()
    private val savedStateHandle: SavedStateHandle = get()

    val allowExperimental = get<PreferencesManager>().allowExperimental
    val bundlesFlow = get<PatchBundleRepository>().sources.flatMapLatestAndCombine(
        combiner = { it.filterNotNull() }
    ) { source ->
        // Regenerate bundle information whenever this source updates.
        source.state.map { state ->
            val bundle = state.patchBundleOrNull() ?: return@map null

            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()
            val universal = mutableListOf<PatchInfo>()

            bundle.patches.filter { it.compatibleWith(input.selectedApp.packageName) }.forEach {
                val targetList = when {
                    it.compatiblePackages == null -> universal
                    it.supportsVersion(input.selectedApp.version) -> supported
                    else -> unsupported
                }

                targetList.add(it)
            }

            BundleInfo(source.name, source.uid, bundle.patches, supported, unsupported, universal)
        }
    }

    /*
    private val selectedPatches: SnapshotStatePatchesSelection by savedStateHandle.saveable(
        saver = userPatchesSelectionSasver,
        init = {
            val map: SnapshotStatePatchesSelection = mutableStateMapOf()
            viewModelScope.launch(Dispatchers.Default) {
                val bundles = bundlesFlow.first()
                val filteredSelection =
                    (input.patchesSelection
                        ?: selectionRepository.getSelection(input.selectedApp.packageName))
                        .mapValues { (uid, patches) ->
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
                    map.putAll(filteredSelection)
                }
            }
            return@saveable map
        })*/
    private val previousPatchSelection: SnapshotStateMap<Int, Set<String>> = mutableStateMapOf()
    init {
        viewModelScope.launch(Dispatchers.Default) {
            // val bundles = bundlesFlow.first()
            val unfilteredSelection = input.patchesSelection ?: selectionRepository.getSelection(input.selectedApp.packageName)
            val filteredSelection = unfilteredSelection.mapValues { (_, value) -> value.toSet() }
            /*
            val filteredSelection = unfilteredSelection.mapValues { (uid, patches) ->
                // Filter out patches that don't exist.
                val filteredPatches = bundles.singleOrNull { it.uid == uid }
                    ?.let { bundle ->
                        val allPatches = bundle.all.map { it.name }
                        patches.filter { allPatches.contains(it) }
                    }
                    ?: patches
                filteredPatches.toSet()
            }*/

            withContext(Dispatchers.Main) {
                previousPatchSelection.putAll(filteredSelection)
            }
        }
    }

    private val userSelection: SnapshotStateUserPatchesSelection by savedStateHandle.saveable(
        saver = userPatchesSelectionSaver,
        init = ::mutableStateMapOf
    )

    private val patchOptions: SnapshotStateOptions by savedStateHandle.saveable(
        saver = optionsSaver,
        init = ::mutableStateMapOf
    )

    private val selectors: List<Selector> = listOf(
        { bundle, patch ->
            userSelection[bundle]?.get(patch.name)
        },
        { bundle, patch ->
            // previousPatchSelection[bundle]?.contains(patch.name)
            null
        },
        { _, patch ->
            patch.include
        }
    )

    /**
     * Show the patch options dialog for this patch.
     */
    var optionsDialog by mutableStateOf<Pair<Int, PatchInfo>?>(null)

    val compatibleVersions = mutableStateListOf<String>()

    var filter by mutableStateOf(SHOW_SUPPORTED or SHOW_UNSUPPORTED)
        private set

    private fun getOrCreateSelection(bundle: Int) =
        userSelection.getOrPut(bundle, ::mutableStateMapOf)

    fun isSelected(bundle: Int, patch: PatchInfo) =
        selectors.firstNotNullOf { fn -> fn(bundle, patch) }

    fun togglePatch(bundle: Int, patch: PatchInfo) {
        val patches = getOrCreateSelection(bundle)

        patches[patch.name] = !isSelected(bundle, patch)
    }

    suspend fun getSelection(): PatchesSelection {
        val bundles = bundlesFlow.first()
        val removeUnsupported = !allowExperimental.get()

        return bundles.associate { bundle ->
            val included = bundle.all.filter { isSelected(bundle.uid, it) }.map { it.name }.toMutableSet()

            if (removeUnsupported) {
                val unsupported = bundle.unsupported.map { it.name }.toSet()
                included.removeAll(unsupported)
            }

            bundle.uid to included
        }
    }

    // TODO: reimplement this.
    /*
    suspend fun getAndSaveSelection(): PatchesSelection =
        selectedPatches.also {
            withContext(Dispatchers.Default) {
                selectionRepository.updateSelection(input.selectedApp.packageName, it)
            }
        }.mapValues { it.value.toMutableSet() }.apply {
            if (allowExperimental.get()) {
                return@apply
            }

            // Filter out unsupported patches that may have gotten selected through the database if the setting is not enabled.
            bundlesFlow.first().forEach {
                this[it.uid]?.removeAll(it.unsupported.map { patch -> patch.name }.toSet())
            }
        }
    */

    fun getOptions(): Options = patchOptions
    fun getOptions(bundle: Int, patch: PatchInfo) = patchOptions[bundle]?.get(patch.name)

    fun setOption(bundle: Int, patch: PatchInfo, key: String, value: Any?) {
        patchOptions.getOrCreate(bundle).getOrCreate(patch.name)[key] = value
    }

    fun resetOptions(bundle: Int, patch: PatchInfo) {
        patchOptions[bundle]?.remove(patch.name)
    }

    fun dismissDialogs() {
        optionsDialog = null
        compatibleVersions.clear()
    }

    fun openUnsupportedDialog(unsupportedVersions: List<PatchInfo>) {
        val set = HashSet<String>()

        unsupportedVersions.forEach { patch ->
            patch.compatiblePackages?.find { it.packageName == input.selectedApp.packageName }
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

        private fun <K, K2, V> SnapshotStateMap<K, SnapshotStateMap<K2, V>>.getOrCreate(key: K) =
            getOrPut(key, ::mutableStateMapOf)

        private val optionsSaver: Saver<SnapshotStateOptions, Options> = snapshotStateMapSaver(
            // Patch name -> Options
            valueSaver = snapshotStateMapSaver(
                // Option key -> Option value
                valueSaver = snapshotStateMapSaver()
            )
        )

        private val userPatchesSelectionSaver: Saver<SnapshotStateUserPatchesSelection, UserPatchesSelection> =
            snapshotStateMapSaver(valueSaver = snapshotStateMapSaver())
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

/**
 * [Options] but with observable collection types.
 */
private typealias SnapshotStateOptions = SnapshotStateMap<Int, SnapshotStateMap<String, SnapshotStateMap<String, Any?>>>

/**
 * [PatchesSelection] but with observable collection types.
 */
private typealias SnapshotStatePatchesSelection = SnapshotStateMap<Int, SnapshotStateSet<String>>

private typealias UserPatchesSelection = Map<Int, Map<String, Boolean>>
private typealias SnapshotStateUserPatchesSelection = SnapshotStateMap<Int, SnapshotStateMap<String, Boolean>>

private typealias Selector = (Int, PatchInfo) -> Boolean?