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
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.ui.model.BundleInfo
import app.revanced.manager.ui.model.BundleInfo.Extensions.toPatchSelection
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchesSelection
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
    private val pm: PM = get()
    private val savedStateHandle: SavedStateHandle = get()
    val prefs: PreferencesManager = get()

    private var _selectedApp by savedStateHandle.saveable {
        mutableStateOf(input.app)
    }

    var selectedApp
        get() = _selectedApp
        set(value) {
            invalidateSelectedAppInfo()
            _selectedApp = value
        }

    var selectedAppInfo: PackageInfo? by mutableStateOf(null)

    init {
        invalidateSelectedAppInfo()
    }

    var patchOptions: Options by savedStateHandle.saveable {
        mutableStateOf(emptyMap())
    }

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

    fun getPatches(bundles: List<BundleInfo>, allowUnsupported: Boolean) =
        selectionState.patches(bundles, allowUnsupported)

    fun getCustomPatches(
        bundles: List<BundleInfo>,
        allowUnsupported: Boolean
    ): PatchesSelection? =
        (selectionState as? SelectionState.Customized)?.patches(bundles, allowUnsupported)

    fun setCustomPatches(selection: PatchesSelection?) {
        selectionState = selection?.let(SelectionState::Customized) ?: SelectionState.Default
    }

    data class Params(
        val app: SelectedApp,
        val patches: PatchesSelection?,
    )
}

private sealed interface SelectionState : Parcelable {
    fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean): PatchesSelection

    @Parcelize
    data class Customized(val patchesSelection: PatchesSelection) : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean) =
            bundles.toPatchSelection(
                allowUnsupported
            ) { uid, patch ->
                patchesSelection[uid]?.contains(patch.name) ?: false
            }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean) =
            bundles.toPatchSelection(allowUnsupported) { _, patch -> patch.include }
    }
}

