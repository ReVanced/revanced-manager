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
import app.revanced.manager.ui.destination.SelectedAppInfoDestination
import app.revanced.manager.ui.model.BundleInfo
import app.revanced.manager.ui.model.bundleInfoFlow
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.SnapshotStateSet
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.saver.snapshotStateMapSaver
import app.revanced.manager.util.saver.snapshotStateSetSaver
import app.revanced.manager.util.toMutableStateSet
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Stable
@OptIn(SavedStateHandleSaveableApi::class)
class PatchesSelectorViewModel(
    // TODO: maybe don't tie this to the destination
    val input: SelectedAppInfoDestination.PatchesSelector
) : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()

    private val packageName = input.app.packageName

    var pendingSelectionAction by mutableStateOf<(() -> Unit)?>(null)

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

            fun BundleInfo.hasDefaultPatches() = sequence(allowExperimental).any { it.include }

            // Don't show the warning if there are no default patches.
            selectionWarningEnabled = bundlesFlow.first().any(BundleInfo::hasDefaultPatches)
        }
    }

    private var hasModifiedSelection = false

    private var wrappedCustomPatchesSelection: Optional<SnapshotPatchesSelection> by savedStateHandle.saveable(
        stateSaver = nullablePatchesSaver,
        init = {
            val currentSelection =
                input.currentSelection?.mapValuesTo(mutableStateMapOf()) { (_, set) -> set.toMutableStateSet() }
            mutableStateOf(Optional.ofNullable(currentSelection))
        }
    )

    private var customPatchesSelection
        get() = wrappedCustomPatchesSelection.getOrNull()
        set(value) {
            wrappedCustomPatchesSelection = Optional.ofNullable(value)
        }

    private val patchOptions: SnapshotOptions by savedStateHandle.saveable(
        saver = optionsSaver,
        init = ::mutableStateMapOf
    )

    /**
     * Show the patch options dialog for this patch.
     */
    var optionsDialog by mutableStateOf<Pair<Int, PatchInfo>?>(null)

    val compatibleVersions = mutableStateListOf<String>()

    var filter by mutableIntStateOf(SHOW_SUPPORTED or SHOW_UNIVERSAL or SHOW_UNSUPPORTED)
        private set

    fun isSelected(bundle: Int, patch: PatchInfo): Boolean {
        return customPatchesSelection?.let {
            it[bundle]?.contains(patch.name)
        } ?: when {
            !patch.include -> false
            !allowExperimental && !patch.supportsVersion(
                packageName,
                input.app.version
            ) -> false

            else -> true
        }
    }

    private suspend fun getOrCreateCustomPatchesSelection() =
        customPatchesSelection.let { patches ->
            if (patches != null) patches else {
                val bundles = bundlesFlow.first()
                val generatedSelection = SelectionState.Default.patches(bundles, allowExperimental)

                generatedSelection.mapValuesTo(
                    mutableStateMapOf()
                ) { (_, patches) ->
                    patches.toMutableStateSet()
                }.also { map ->
                    customPatchesSelection = map
                }
            }
        }

    fun togglePatch(bundle: Int, patch: PatchInfo) = viewModelScope.launch {
        val patches = getOrCreateCustomPatchesSelection().getOrPut(bundle, ::mutableStateSetOf)

        hasModifiedSelection = true
        if (patch.name in patches) patches.remove(patch.name) else patches.add(patch.name)
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
        // baseSelectionMode = BaseSelectionMode.DEFAULT
        customPatchesSelection = null
        hasModifiedSelection = false
        app.toast(app.getString(R.string.patch_selection_reset_toast))
    }

    fun getSelection(): PatchesSelection? {
        return customPatchesSelection?.mapValuesTo(mutableMapOf()) { (_, v) -> v.toMutableSet() }
        /*
        val bundles = bundlesFlow.first()

        return bundles.associate { bundle ->
            val included =
                bundle.all.filter { isSelected(bundle.uid, it) }.map { it.name }.toMutableSet()

            if (!allowExperimental) {
                val unsupported = bundle.unsupported.map { it.name }.toSet()
                included.removeAll(unsupported)
            }

            bundle.uid to included
        }*/
    }

    suspend fun saveSelection(selection: PatchesSelection) =
        viewModelScope.launch(Dispatchers.Default) {
            // TODO: implement
            /*
            when {
                hasModifiedSelection -> selectionRepository.updateSelection(packageName, selection)
                baseSelectionMode == BaseSelectionMode.DEFAULT -> selectionRepository.clearSelection(
                    packageName
                )

                else -> {}
            }
            */
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

        private fun <K, K2, V> SnapshotStateMap<K, SnapshotStateMap<K2, V>>.getOrCreate(key: K) =
            getOrPut(key, ::mutableStateMapOf)

        private val optionsSaver: Saver<SnapshotOptions, Options> = snapshotStateMapSaver(
            // Patch name -> Options
            valueSaver = snapshotStateMapSaver(
                // Option key -> Option value
                valueSaver = snapshotStateMapSaver()
            )
        )

        private val patchesSaver: Saver<SnapshotPatchesSelection, PatchesSelection> =
            snapshotStateMapSaver(valueSaver = snapshotStateSetSaver())

        // i have no idea if this is actually necessary
        private val nullablePatchesSaver: Saver<Optional<SnapshotPatchesSelection>, Optional<PatchesSelection>> =
            Saver(
                save = { value ->
                    val patches = value.getOrNull() ?: return@Saver Optional.empty()
                    val saved = with(patchesSaver) {
                        save(patches)
                    }
                    saved?.let {
                        Optional.of(it)
                    }
                },
                restore = {
                    Optional.ofNullable(it.getOrNull()?.let(patchesSaver::restore))
                }
            )
    }
}

// Versions of other types, but utilizing observable collection types instead.
private typealias SnapshotOptions = SnapshotStateMap<Int, SnapshotStateMap<String, SnapshotStateMap<String, Any?>>>
private typealias SnapshotPatchesSelection = SnapshotStateMap<Int, SnapshotStateSet<String>>