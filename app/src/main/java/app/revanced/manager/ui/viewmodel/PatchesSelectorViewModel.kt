package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.flatMapLatestAndCombine
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.toast
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
    private val app: Application = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()

    private val packageName = input.selectedApp.packageName

    var pendingSelectionAction by mutableStateOf<(() -> Unit)?>(null)

    var selectionWarningEnabled by mutableStateOf(true)
        private set

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

            bundle.patches.filter { it.compatibleWith(packageName) }.forEach {
                val targetList = when {
                    it.compatiblePackages == null -> universal
                    it.supportsVersion(
                        input.selectedApp.packageName,
                        input.selectedApp.version
                    ) -> supported

                    else -> unsupported
                }

                targetList.add(it)
            }

            BundleInfo(source.name, source.uid, bundle.patches, supported, unsupported, universal)
        }
    }

    init {
        viewModelScope.launch {
            if (prefs.disableSelectionWarning.get()) {
                selectionWarningEnabled = false
                return@launch
            }

            val experimental = allowExperimental.get()
            fun BundleInfo.hasDefaultPatches(): Boolean {
                return if (experimental) {
                    all.asSequence()
                } else {
                    sequence {
                        yieldAll(supported)
                        yieldAll(universal)
                    }
                }.any { it.include }
            }

            // Don't show the warning if there are no default patches.
            selectionWarningEnabled = bundlesFlow.first().any(BundleInfo::hasDefaultPatches)
        }
    }

    var baseSelectionMode by mutableStateOf(BaseSelectionMode.DEFAULT)
        private set

    private val previousPatchesSelection: SnapshotStateMap<Int, Set<String>> = mutableStateMapOf()

    init {
        viewModelScope.launch(Dispatchers.Default) { loadPreviousSelection() }
    }

    val hasPreviousSelection by derivedStateOf {
        previousPatchesSelection.filterValues(Set<String>::isNotEmpty).isNotEmpty()
    }

    private var hasModifiedSelection = false

    private val explicitPatchesSelection: SnapshotExplicitPatchesSelection by savedStateHandle.saveable(
        saver = explicitPatchesSelectionSaver,
        init = ::mutableStateMapOf
    )

    private val patchOptions: SnapshotOptions by savedStateHandle.saveable(
        saver = optionsSaver,
        init = ::mutableStateMapOf
    )

    private val selectors by derivedStateOf<Array<Selector>> {
        arrayOf(
            // Patches that were explicitly selected
            { bundle, patch ->
                explicitPatchesSelection[bundle]?.get(patch.name)
            },
            // The fallback selection.
            when (baseSelectionMode) {
                BaseSelectionMode.DEFAULT -> ({ _, patch -> patch.include })

                BaseSelectionMode.PREVIOUS -> ({ bundle, patch ->
                    previousPatchesSelection[bundle]?.contains(patch.name) ?: false
                })
            }
        )
    }

    /**
     * Show the patch options dialog for this patch.
     */
    var optionsDialog by mutableStateOf<Pair<Int, PatchInfo>?>(null)

    val compatibleVersions = mutableStateListOf<String>()

    var filter by mutableStateOf(SHOW_SUPPORTED or SHOW_UNIVERSAL or SHOW_UNSUPPORTED)
        private set

    private suspend fun loadPreviousSelection() {
        val selection = (input.patchesSelection ?: selectionRepository.getSelection(
            packageName
        )).mapValues { (_, value) -> value.toSet() }

        withContext(Dispatchers.Main) {
            previousPatchesSelection.putAll(selection)
        }
    }

    fun switchBaseSelectionMode() = viewModelScope.launch {
        baseSelectionMode = if (baseSelectionMode == BaseSelectionMode.DEFAULT) {
            BaseSelectionMode.PREVIOUS
        } else {
            BaseSelectionMode.DEFAULT
        }
    }

    private suspend fun patchesAvailable(bundle: BundleInfo): List<PatchInfo> {
        val patches = (bundle.supported + bundle.universal).toMutableList()
        val removeUnsupported = !allowExperimental.get()
        if (!removeUnsupported) patches += bundle.unsupported
        return patches
    }

    suspend fun isSelectionNotEmpty() =
        bundlesFlow.first().any { bundle ->
            patchesAvailable(bundle).any { patch ->
                isSelected(bundle.uid, patch)
            }
        }

    private fun getOrCreateSelection(bundle: Int) =
        explicitPatchesSelection.getOrPut(bundle, ::mutableStateMapOf)

    fun isSelected(bundle: Int, patch: PatchInfo) =
        selectors.firstNotNullOf { fn -> fn(bundle, patch) }

    fun togglePatch(bundle: Int, patch: PatchInfo) {
        val patches = getOrCreateSelection(bundle)

        hasModifiedSelection = true
        patches[patch.name] = !isSelected(bundle, patch)
    }

    fun confirmSelectionWarning(dismissPermanently: Boolean) {
        selectionWarningEnabled = false

        pendingSelectionAction?.invoke()
        pendingSelectionAction = null

        if (!dismissPermanently) return

        viewModelScope.launch {
            prefs.disableSelectionWarning.update(true)
        }
    }

    fun dismissSelectionWarning() {
        pendingSelectionAction = null
    }

    fun reset() {
        patchOptions.clear()
        baseSelectionMode = BaseSelectionMode.DEFAULT
        explicitPatchesSelection.clear()
        hasModifiedSelection = false
        app.toast(app.getString(R.string.patch_selection_reset_toast))
    }

    suspend fun getSelection(): PatchesSelection {
        val bundles = bundlesFlow.first()
        val removeUnsupported = !allowExperimental.get()

        return bundles.associate { bundle ->
            val included =
                bundle.all.filter { isSelected(bundle.uid, it) }.map { it.name }.toMutableSet()

            if (removeUnsupported) {
                val unsupported = bundle.unsupported.map { it.name }.toSet()
                included.removeAll(unsupported)
            }

            bundle.uid to included
        }
    }

    suspend fun saveSelection(selection: PatchesSelection) =
        viewModelScope.launch(Dispatchers.Default) {
            when {
                hasModifiedSelection -> selectionRepository.updateSelection(packageName, selection)
                baseSelectionMode == BaseSelectionMode.DEFAULT -> selectionRepository.clearSelection(
                    packageName
                )

                else -> {}
            }
        }

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

    fun openUnsupportedDialog(unsupportedPatches: List<PatchInfo>) {
        compatibleVersions.addAll(unsupportedPatches.flatMap { patch ->
            patch.compatiblePackages?.find { it.packageName == input.selectedApp.packageName }?.versions.orEmpty()
        })
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

        private val optionsSaver: Saver<SnapshotOptions, Options> = snapshotStateMapSaver(
            // Patch name -> Options
            valueSaver = snapshotStateMapSaver(
                // Option key -> Option value
                valueSaver = snapshotStateMapSaver()
            )
        )

        private val explicitPatchesSelectionSaver: Saver<SnapshotExplicitPatchesSelection, ExplicitPatchesSelection> =
            snapshotStateMapSaver(valueSaver = snapshotStateMapSaver())
    }

    /**
     * An enum for controlling the behavior of the selector.
     */
    enum class BaseSelectionMode {
        /**
         * Selection is determined by the [PatchInfo.include] field.
         */
        DEFAULT,

        /**
         * Selection is determined by what the user selected previously.
         * Any patch that is not part of the previous selection will be deselected.
         */
        PREVIOUS
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

private typealias Selector = (Int, PatchInfo) -> Boolean?
private typealias ExplicitPatchesSelection = Map<Int, Map<String, Boolean>>

// Versions of other types, but utilizing observable collection types instead.
private typealias SnapshotOptions = SnapshotStateMap<Int, SnapshotStateMap<String, SnapshotStateMap<String, Any?>>>
private typealias SnapshotExplicitPatchesSelection = SnapshotStateMap<Int, SnapshotStateMap<String, Boolean>>