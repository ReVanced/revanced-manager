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
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.model.BundleInfo
import app.revanced.manager.ui.model.BundleInfo.Extensions.bundleInfoFlow
import app.revanced.manager.ui.model.BundleInfo.Extensions.toPatchSelection
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.saver.nullableSaver
import app.revanced.manager.util.saver.persistentMapSaver
import app.revanced.manager.util.saver.persistentSetSaver
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlinx.collections.immutable.*
import kotlinx.coroutines.withContext
import java.util.Optional

@Stable
@OptIn(SavedStateHandleSaveableApi::class)
class PatchesSelectorViewModel(input: Params) : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()

    private val packageName = input.app.packageName
    val appVersion = input.app.version

    var pendingSelectionAction by mutableStateOf<(() -> Unit)?>(null)

    // TODO: this should be hoisted to the parent screen
    var selectionWarningEnabled by mutableStateOf(true)
        private set

    val allowExperimental = get<PreferencesManager>().allowExperimental.getBlocking()
    val bundlesFlow =
        get<PatchBundleRepository>().bundleInfoFlow(packageName, input.app.version)

    init {
        viewModelScope.launch {
            if (prefs.disableSelectionWarning.get()) {
                selectionWarningEnabled = false
                return@launch
            }

            fun BundleInfo.hasDefaultPatches() = patchSequence(allowExperimental).any { it.include }

            // Don't show the warning if there are no default patches.
            selectionWarningEnabled = bundlesFlow.first().any(BundleInfo::hasDefaultPatches)
        }
    }

    private var hasModifiedSelection = false
    private var customPatchesSelection: PersistentPatchesSelection? by savedStateHandle.saveable(
        key = "selection",
        stateSaver = patchesSaver,
    ) {
        mutableStateOf(input.currentSelection?.toPersistentPatchesSelection())
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

    private suspend fun generateDefaultSelection(): PersistentPatchesSelection {
        val bundles = bundlesFlow.first()
        val generatedSelection =
            bundles.toPatchSelection(allowExperimental) { _, patch -> patch.include }

        return generatedSelection.toPersistentPatchesSelection()
    }

    fun selectionIsValid(bundles: List<BundleInfo>) = bundles.any { bundle ->
        bundle.patchSequence(allowExperimental).any { patch ->
            isSelected(bundle.uid, patch)
        }
    }

    fun isSelected(bundle: Int, patch: PatchInfo) = customPatchesSelection?.let { selection ->
        selection[bundle]?.contains(patch.name) ?: false
    } ?: patch.include

    fun togglePatch(bundle: Int, patch: PatchInfo) = viewModelScope.launch {
        hasModifiedSelection = true

        val selection = customPatchesSelection ?: generateDefaultSelection()
        val newPatches = selection[bundle]?.let { patches ->
            if (patch.name in patches)
                patches.remove(patch.name)
            else
                patches.add(patch.name)
        } ?: persistentSetOf(patch.name)

        customPatchesSelection = selection.put(bundle, newPatches)
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
        customPatchesSelection = null
        hasModifiedSelection = false
        app.toast(app.getString(R.string.patch_selection_reset_toast))
    }

    fun getCustomSelection(): PatchesSelection? {
        // Convert persistent collections to standard hash collections because persistent collections are not parcelable.

        return customPatchesSelection?.mapValues { (_, v) -> v.toSet() }
    }

    fun getOptions(): Options {
        // Convert the collection for the same reasons as in getCustomSelection()

        return patchOptions.mapValues { (_, allPatches) -> allPatches.mapValues { (_, options) -> options.toMap() } }
    }

    suspend fun saveSelection() = withContext(Dispatchers.Default) {
        customPatchesSelection?.let { selectionRepository.updateSelection(packageName, it) }
            ?: selectionRepository.clearSelection(packageName)
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
        patchOptions[bundle]?.remove(patch.name)
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

        private val patchesSaver: Saver<PersistentPatchesSelection?, Optional<PatchesSelection>> =
            nullableSaver(persistentMapSaver(valueSaver = persistentSetSaver()))
    }

    data class Params(
        val app: SelectedApp,
        val currentSelection: PatchesSelection?,
        val options: Options,
    )
}

// Versions of other types, but utilizing persistent/observable collection types.
private typealias PersistentOptions = SnapshotStateMap<Int, PersistentMap<String, PersistentMap<String, Any?>>>
private typealias PersistentPatchesSelection = PersistentMap<Int, PersistentSet<String>>

private fun PatchesSelection.toPersistentPatchesSelection(): PersistentPatchesSelection =
    mapValues { (_, v) -> v.toPersistentSet() }.toPersistentMap()