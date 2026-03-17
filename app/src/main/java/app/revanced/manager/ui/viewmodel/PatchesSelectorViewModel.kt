package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.revanced.manager.R
import app.revanced.manager.domain.sources.PatchBundleSource
import app.revanced.manager.domain.sources.Source.State
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.sources.Extensions.version
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.toPatchSelection
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.model.navigation.SelectedApplicationInfo
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.saver.Nullable
import app.revanced.manager.util.saver.nullableSaver
import app.revanced.manager.util.saver.persistentMapSaver
import app.revanced.manager.util.saver.persistentSetSaver
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.toast
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(SavedStateHandleSaveableApi::class)
class PatchesSelectorViewModel(input: SelectedApplicationInfo.PatchesSelector.ViewModelParams) :
    ViewModel(), KoinComponent {
    private val app: Application = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()
    private val bundleRepository: PatchBundleRepository = get()

    val readOnly = input.readOnly
    private val browseAllBundles = input.browseAllBundles
    val packageName = input.app.packageName
    val appVersion = input.app.version

    var selectionWarningEnabled by mutableStateOf(true)
        private set
    var universalPatchWarningEnabled by mutableStateOf(true)
        private set

    val allowIncompatiblePatches =
        get<PreferencesManager>().disablePatchVersionCompatCheck.getBlocking() || appVersion == null
    val bundlesFlow = if (browseAllBundles) {
        combine(bundleRepository.sources, bundleRepository.bundleInfoFlow) { sources, bundles ->
            mergeSourcesWithBundleInfo(
                sources,
                bundles.mapValues { (_, bundle) -> bundle.asReadonlyScoped() }
            )
        }
    } else {
        combine(
            bundleRepository.sources,
            bundleRepository.scopedBundleInfoFlow(packageName, input.app.version)
        ) { sources, bundles ->
            mergeSourcesWithBundleInfo(
                sources,
                bundles.associateBy(PatchBundleInfo.Scoped::uid)
            )
        }
    }

    val bundleLoadIssuesFlow = bundleRepository.sources.map { sources ->
        sources.mapNotNull { source ->
            val messageId = when {
                source.error != null -> R.string.patches_error_description
                source.state is State.Missing -> R.string.patches_not_downloaded
                else -> null
            } ?: return@mapNotNull null

            source.uid to messageId
        }.toMap()
    }

    init {
        viewModelScope.launch {
            if (readOnly) {
                universalPatchWarningEnabled = false
                selectionWarningEnabled = false
                return@launch
            }

            if (prefs.disableUniversalPatchCheck.get()) {
                universalPatchWarningEnabled = false
            }

            if (prefs.disableSelectionWarning.get()) {
                selectionWarningEnabled = false
                return@launch
            }

            fun PatchBundleInfo.Scoped.hasDefaultPatches() =
                patchSequence(allowIncompatiblePatches).any { it.include }

            // Don't show the warning if there are no default patches.
            selectionWarningEnabled = bundlesFlow.first().any(PatchBundleInfo.Scoped::hasDefaultPatches)
        }
    }

    private var hasModifiedSelection = false
    var customPatchSelection: PersistentPatchSelection? by savedStateHandle.saveable(
        key = "selection",
        stateSaver = selectionSaver,
    ) {
        mutableStateOf(input.currentSelection?.toPersistentPatchSelection())
    }

    private val patchOptions: PersistentOptions by savedStateHandle.saveable(
        saver = optionsSaver,
    ) {
        // Convert Options to PersistentOptions
        input.options.mapValuesTo(mutableStateMapOf()) { (_, allPatches) ->
            allPatches.mapValues { (_, options) -> options.toPersistentMap() }.toPersistentMap()
        }
    }

    /**
     * Active dialog or bottom sheet state. Only one can be shown at a time.
     */
    var activeDialog by mutableStateOf<DialogState?>(null)
        private set

    var filter by mutableIntStateOf(SHOW_UNIVERSAL)
        private set

    private val defaultPatchSelection = bundlesFlow.map { bundles ->
        bundles.toPatchSelection(allowIncompatiblePatches) { _, patch -> patch.include }
            .toPersistentPatchSelection()
    }

    val defaultSelectionCount = defaultPatchSelection.map { selection ->
        selection.values.sumOf { it.size }
    }

    // This is for the required options screen.
    private val requiredOptsPatchesDeferred = viewModelScope.async(start = CoroutineStart.LAZY) {
        bundlesFlow.first().map { bundle ->
            bundle to bundle.patchSequence(allowIncompatiblePatches).filter { patch ->
                val opts by lazy {
                    getOptions(bundle.uid, patch).orEmpty()
                }
                isSelected(
                    bundle.uid,
                    patch
                ) && patch.options?.any { it.required && it.default == null && it.name !in opts } ?: false
            }.toList()
        }.filter { (_, patches) -> patches.isNotEmpty() }
    }
    val requiredOptsPatches = flow { emit(requiredOptsPatchesDeferred.await()) }

    fun selectionIsValid(bundles: List<PatchBundleInfo.Scoped>) = !readOnly && bundles.any { bundle ->
        bundle.patchSequence(allowIncompatiblePatches).any { patch ->
            isSelected(bundle.uid, patch)
        }
    }

    fun isSelected(bundle: Int, patch: PatchInfo) = customPatchSelection?.let { selection ->
        selection[bundle]?.contains(patch.name) == true
    } ?: patch.include

    fun togglePatch(bundle: Int, patch: PatchInfo) = viewModelScope.launch {
        hasModifiedSelection = true

        val selection = customPatchSelection ?: defaultPatchSelection.first()
        val newPatches = selection[bundle]?.let { patches ->
            if (patch.name in patches)
                patches.remove(patch.name)
            else
                patches.add(patch.name)
        } ?: persistentSetOf(patch.name)

        customPatchSelection = selection.put(bundle, newPatches)
    }

    fun reset() {
        patchOptions.clear()
        customPatchSelection = null
        hasModifiedSelection = false
        app.toast(app.getString(R.string.patch_selection_reset_toast))
    }

    fun getCustomSelection(): PatchSelection? {
        // Convert persistent collections to standard hash collections because persistent collections are not parcelable.

        return customPatchSelection?.mapValues { (_, v) -> v.toSet() }
    }

    fun getOptions(): Options {
        // Convert the collection for the same reasons as in getCustomSelection()

        return patchOptions.mapValues { (_, allPatches) -> allPatches.mapValues { (_, options) -> options.toMap() } }
    }

    fun getOptions(bundle: Int, patch: PatchInfo) = patchOptions[bundle]?.get(patch.name)

    fun setOption(bundle: Int, patch: PatchInfo, name: String, value: Any?) {
        // All patches
        val patchesToOpts = patchOptions.getOrElse(bundle, ::persistentMapOf)
        // The key-value options of an individual patch
        val patchToOpts = patchesToOpts
            .getOrElse(patch.name, ::persistentMapOf)
            .put(name, value)

        patchOptions[bundle] = patchesToOpts.put(patch.name, patchToOpts)
    }

    fun resetOptions(bundle: Int, patch: PatchInfo) {
        app.toast(app.getString(R.string.patch_options_reset_toast))
        patchOptions[bundle] = patchOptions[bundle]?.remove(patch.name) ?: return
    }

    fun dismissDialogs() {
        activeDialog = null
    }

    fun openIncompatibleDialog(incompatiblePatch: PatchInfo) {
        val versions = incompatiblePatch.compatiblePackages
            ?.find { it.packageName == packageName }?.versions.orEmpty()
        activeDialog = DialogState.IncompatiblePatch(versions)
    }

    fun openOptionsDialog(bundle: Int, patch: PatchInfo) {
        activeDialog = DialogState.Options(bundle, patch)
    }

    fun showSelectionWarning() {
        activeDialog = DialogState.SelectionWarning
    }

    fun showUniversalPatchWarning() {
        activeDialog = DialogState.UniversalPatchWarning
    }

    fun showIncompatiblePatchesInfo() {
        activeDialog = DialogState.IncompatiblePatchesInfo
    }

    fun toggleFlag(flag: Int) {
        filter = filter xor flag
    }

    fun getBundleSelectionState(bundle: PatchBundleInfo.Scoped): Boolean? {
        val patches = bundle.patchSequence(allowIncompatiblePatches).toList()
        if (patches.isEmpty()) return false

        val selectedCount = patches.count { isSelected(bundle.uid, it) }
        return when (selectedCount) {
            patches.size -> true
            0 -> false
            else -> null
        }
    }

    private suspend fun currentSelection(): PersistentPatchSelection =
        customPatchSelection ?: defaultPatchSelection.first()

    private suspend fun updateSelection(
        update: (PersistentPatchSelection) -> PersistentPatchSelection
    ) {
        hasModifiedSelection = true
        customPatchSelection = update(currentSelection())
    }

    fun deselectAll(bundles: List<PatchBundleInfo.Scoped>, bundleUid: Int?) = viewModelScope.launch {
        updateSelection { selection ->
            bundles.fold(selection) { acc, bundle ->
                if (bundleUid != null && bundle.uid != bundleUid) return@fold acc
                acc.put(bundle.uid, persistentSetOf())
            }
        }
    }

    fun invertSelection(bundles: List<PatchBundleInfo.Scoped>, bundleUid: Int?) = viewModelScope.launch {
        updateSelection { selection ->
            bundles.fold(selection) { acc, bundle ->
                if (bundleUid != null && bundle.uid != bundleUid) return@fold acc

                val currentSelected = acc[bundle.uid] ?: persistentSetOf()
                val inverted = bundle.patchSequence(allowIncompatiblePatches)
                    .filter { it.name !in currentSelected }
                    .map { it.name }
                    .toPersistentSet()
                acc.put(bundle.uid, inverted)
            }
        }
    }

    fun restoreDefaults(bundleUid: Int?) = viewModelScope.launch {
        if (bundleUid == null) {
            customPatchSelection = null
            hasModifiedSelection = false
            return@launch
        }

        val defaults = defaultPatchSelection.first()
        updateSelection { selection ->
            selection.put(bundleUid, defaults[bundleUid] ?: persistentSetOf())
        }
    }

    fun deselectAllExcept(bundles: List<PatchBundleInfo.Scoped>, keepBundleUid: Int) = viewModelScope.launch {
        updateSelection { selection ->
            bundles.fold(selection) { acc, bundle ->
                if (bundle.uid == keepBundleUid) return@fold acc
                acc.put(bundle.uid, persistentSetOf())
            }
        }
    }

    companion object {
        const val SHOW_INCOMPATIBLE = 1 // 2^0
        const val SHOW_UNIVERSAL = 2 // 2^1

        private val optionsSaver: Saver<PersistentOptions, Options> = snapshotStateMapSaver(
            // Patch name -> Options
            valueSaver = persistentMapSaver(
                // Option key -> Option value
                valueSaver = persistentMapSaver()
            )
        )

        private val selectionSaver: Saver<PersistentPatchSelection?, Nullable<PatchSelection>> =
            nullableSaver(persistentMapSaver(valueSaver = persistentSetSaver()))
    }

    sealed interface DialogState {
        data class Options(val bundle: Int, val patch: PatchInfo) : DialogState
        data class IncompatiblePatch(val compatibleVersions: Set<String>) : DialogState
        data object IncompatiblePatchesInfo : DialogState
        data object SelectionWarning : DialogState
        data object UniversalPatchWarning : DialogState
    }

    private fun mergeSourcesWithBundleInfo(
        sources: List<PatchBundleSource>,
        scopedBundleInfoByUid: Map<Int, PatchBundleInfo.Scoped>
    ) = sources.map { source ->
        scopedBundleInfoByUid[source.uid] ?: source.emptyScopedBundleInfo()
    }
}

// Versions of other types, but utilizing persistent/observable collection types.
private typealias PersistentOptions = SnapshotStateMap<Int, PersistentMap<String, PersistentMap<String, Any?>>>
private typealias PersistentPatchSelection = PersistentMap<Int, PersistentSet<String>>

private fun PatchSelection.toPersistentPatchSelection(): PersistentPatchSelection =
    mapValues { (_, v) -> v.toPersistentSet() }.toPersistentMap()

private fun PatchBundleInfo.Global.asReadonlyScoped() = PatchBundleInfo.Scoped(
    name = name,
    version = version,
    uid = uid,
    patches = patches,
    compatible = patches,
    incompatible = emptyList(),
    universal = emptyList()
)

private fun PatchBundleSource.emptyScopedBundleInfo() = PatchBundleInfo.Scoped(
    name = name,
    version = version,
    uid = uid,
    patches = emptyList(),
    compatible = emptyList(),
    incompatible = emptyList(),
    universal = emptyList()
)
