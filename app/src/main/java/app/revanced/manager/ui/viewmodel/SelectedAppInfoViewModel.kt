package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.toPatchSelection
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(SavedStateHandleSaveableApi::class)
class SelectedAppInfoViewModel(input: Params) : ViewModel(), KoinComponent {
    val bundlesRepo: PatchBundleRepository = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val optionsRepository: PatchOptionsRepository = get()
    private val pm: PM = get()
    private val savedStateHandle: SavedStateHandle = get()
    val prefs: PreferencesManager = get()

    private val persistConfiguration = input.patches == null

    private var _selectedApp by savedStateHandle.saveable {
        mutableStateOf(input.app)
    }

    var selectedApp
        get() = _selectedApp
        set(value) {
            _selectedApp = value
            invalidateSelectedAppInfo()
        }

    var selectedAppInfo: PackageInfo? by mutableStateOf(null)

    init {
        invalidateSelectedAppInfo()
    }

    var options: Options by savedStateHandle.saveable {
        val state = mutableStateOf<Options>(emptyMap())

        viewModelScope.launch {
            if (!persistConfiguration) return@launch // TODO: save options for patched apps.

            val packageName = selectedApp.packageName // Accessing this from another thread may cause crashes.
            state.value = withContext(Dispatchers.Default) { optionsRepository.getOptions(packageName) }
        }

        state
    }
        private set

    private var selectionState by savedStateHandle.saveable {
        if (input.patches != null) {
            return@saveable mutableStateOf(SelectionState.Customized(input.patches))
        }

        val selection: MutableState<SelectionState> = mutableStateOf(SelectionState.Default)

        // Get previous selection (if present).
        viewModelScope.launch {
            val previous = selectionRepository.getSelection(selectedApp.packageName)

            if (previous.values.sumOf { it.size } == 0) {
                return@launch
            }

            selection.value = SelectionState.Customized(previous)
        }

        selection
    }

    private fun invalidateSelectedAppInfo() = viewModelScope.launch {
        val info = when (val app = selectedApp) {
            is SelectedApp.Download -> null
            is SelectedApp.Local -> withContext(Dispatchers.IO) { pm.getPackageInfo(app.file) }
            is SelectedApp.Installed -> withContext(Dispatchers.IO) { pm.getPackageInfo(app.packageName) }
        }

        selectedAppInfo = info
    }

    fun getOptionsFiltered(bundles: List<PatchBundleInfo.Scoped>) = options.filtered(bundles)

    fun getPatches(bundles: List<PatchBundleInfo.Scoped>, allowUnsupported: Boolean) =
        selectionState.patches(bundles, allowUnsupported)

    fun getCustomPatches(
        bundles: List<PatchBundleInfo.Scoped>,
        allowUnsupported: Boolean
    ): PatchSelection? =
        (selectionState as? SelectionState.Customized)?.patches(bundles, allowUnsupported)

    fun updateConfiguration(
        selection: PatchSelection?,
        options: Options,
        bundles: List<PatchBundleInfo.Scoped>
    ) {
        selectionState = selection?.let(SelectionState::Customized) ?: SelectionState.Default

        val filteredOptions = options.filtered(bundles)
        this.options = filteredOptions

        if (!persistConfiguration) return

        val packageName = selectedApp.packageName
        viewModelScope.launch(Dispatchers.Default) {
            selection?.let { selectionRepository.updateSelection(packageName, it) }
                ?: selectionRepository.clearSelection(packageName)

            optionsRepository.saveOptions(packageName, filteredOptions)
        }
    }

    data class Params(
        val app: SelectedApp,
        val patches: PatchSelection?,
    )

    private companion object {
        /**
         * Returns a copy with all nonexistent options removed.
         */
        private fun Options.filtered(bundles: List<PatchBundleInfo.Scoped>): Options = buildMap options@{
            bundles.forEach bundles@{ bundle ->
                val bundleOptions = this@filtered[bundle.uid] ?: return@bundles

                val patches = bundle.patches.associateBy { it.name }

                this@options[bundle.uid] = buildMap bundleOptions@{
                    bundleOptions.forEach patch@{ (patchName, values) ->
                        // Get all valid option keys for the patch.
                        val validOptionKeys =
                            patches[patchName]?.options?.map { it.key }?.toSet() ?: return@patch

                        this@bundleOptions[patchName] = values.filterKeys { key ->
                            key in validOptionKeys
                        }
                    }
                }
            }
        }
    }
}

private sealed interface SelectionState : Parcelable {
    fun patches(bundles: List<PatchBundleInfo.Scoped>, allowUnsupported: Boolean): PatchSelection

    @Parcelize
    data class Customized(val patchSelection: PatchSelection) : SelectionState {
        override fun patches(bundles: List<PatchBundleInfo.Scoped>, allowUnsupported: Boolean) =
            bundles.toPatchSelection(
                allowUnsupported
            ) { uid, patch ->
                patchSelection[uid]?.contains(patch.name) ?: false
            }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<PatchBundleInfo.Scoped>, allowUnsupported: Boolean) =
            bundles.toPatchSelection(allowUnsupported) { _, patch -> patch.include }
    }
}

