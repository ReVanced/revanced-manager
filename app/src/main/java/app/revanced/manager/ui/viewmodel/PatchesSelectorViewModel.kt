package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.toPatchSelection
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.saver.Nullable
import app.revanced.manager.util.saver.nullableSaver
import app.revanced.manager.util.saver.persistentMapSaver
import app.revanced.manager.util.saver.persistentSetSaver
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlinx.collections.immutable.*

@Stable
@OptIn(SavedStateHandleSaveableApi::class)
class PatchesSelectorViewModel(input: Params) : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()

    private val packageName = input.app.packageName
    val appVersion = input.app.version

    var pendingSelectionAction by mutableStateOf<(() -> Unit)?>(null)

    var selectionWarningEnabled by mutableStateOf(true)
        private set

    val allowIncompatiblePatches =
        get<PreferencesManager>().disablePatchVersionCompatCheck.getBlocking()
    val bundlesFlow =
        get<PatchBundleRepository>().scopedBundleInfoFlow(packageName, input.app.version)

    init {
        viewModelScope.launch {
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
    private var customPatchSelection: PersistentPatchSelection? by savedStateHandle.saveable(
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
     * Show the patch options dialog for this patch.
     */
    var optionsDialog by mutableStateOf<Pair<Int, PatchInfo>?>(null)

    val compatibleVersions = mutableStateListOf<String>()

    var filter by mutableIntStateOf(SHOW_SUPPORTED or SHOW_UNIVERSAL or SHOW_UNSUPPORTED)
        private set

    private suspend fun generateDefaultSelection(): PersistentPatchSelection {
        val bundles = bundlesFlow.first()
        val generatedSelection =
            bundles.toPatchSelection(allowIncompatiblePatches) { _, patch -> patch.include }

        return generatedSelection.toPersistentPatchSelection()
    }

    fun selectionIsValid(bundles: List<PatchBundleInfo.Scoped>) = bundles.any { bundle ->
        bundle.patchSequence(allowIncompatiblePatches).any { patch ->
            isSelected(bundle.uid, patch)
        }
    }

    fun isSelected(bundle: Int, patch: PatchInfo) = customPatchSelection?.let { selection ->
        selection[bundle]?.contains(patch.name) ?: false
    } ?: patch.include

    fun togglePatch(bundle: Int, patch: PatchInfo) = viewModelScope.launch {
        hasModifiedSelection = true

        val selection = customPatchSelection ?: generateDefaultSelection()
        val newPatches = selection[bundle]?.let { patches ->
            if (patch.name in patches)
                patches.remove(patch.name)
            else
                patches.add(patch.name)
        } ?: persistentSetOf(patch.name)

        customPatchSelection = selection.put(bundle, newPatches)
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

    fun setOption(bundle: Int, patch: PatchInfo, key: String, value: Any?) {
        // All patches
        val patchesToOpts = patchOptions.getOrElse(bundle, ::persistentMapOf)
        // The key-value options of an individual patch
        val patchToOpts = patchesToOpts
            .getOrElse(patch.name, ::persistentMapOf)
            .put(key, value)

        patchOptions[bundle] = patchesToOpts.put(patch.name, patchToOpts)
    }

    fun resetOptions(bundle: Int, patch: PatchInfo) {
        app.toast(app.getString(R.string.patch_options_reset_toast))
        patchOptions[bundle] = patchOptions[bundle]?.remove(patch.name) ?: return
    }

    fun dismissDialogs() {
        optionsDialog = null
        compatibleVersions.clear()
    }

    fun openUnsupportedDialog(unsupportedPatches: List<PatchInfo>) {
        compatibleVersions.addAll(unsupportedPatches.flatMap { patch ->
            patch.compatiblePackages?.find { it.packageName == packageName }?.versions.orEmpty()
        })
    }

    fun toggleFlag(flag: Int) {
        filter = filter xor flag
    }

    companion object {
        const val SHOW_SUPPORTED = 1 // 2^0
        const val SHOW_UNIVERSAL = 2 // 2^1
        const val SHOW_UNSUPPORTED = 4 // 2^2

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

    data class Params(
        val app: SelectedApp,
        val currentSelection: PatchSelection?,
        val options: Options,
    )
}

// Versions of other types, but utilizing persistent/observable collection types.
private typealias PersistentOptions = SnapshotStateMap<Int, PersistentMap<String, PersistentMap<String, Any?>>>
private typealias PersistentPatchSelection = PersistentMap<Int, PersistentSet<String>>

private fun PatchSelection.toPersistentPatchSelection(): PersistentPatchSelection =
    mapValues { (_, v) -> v.toPersistentSet() }.toPersistentMap()