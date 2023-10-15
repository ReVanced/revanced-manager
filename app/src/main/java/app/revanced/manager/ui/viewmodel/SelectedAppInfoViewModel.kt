package app.revanced.manager.ui.viewmodel

import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.model.BundleInfo
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class KoinShallExplode(
    val app:SelectedApp,
    val patches: PatchesSelection?
)

@OptIn(SavedStateHandleSaveableApi::class)
class SelectedAppInfoViewModel(input: KoinShallExplode) : ViewModel(), KoinComponent {
    val bundlesRepo: PatchBundleRepository = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val savedStateHandle: SavedStateHandle = get()
    val prefs: PreferencesManager = get()

    var selectedApp by savedStateHandle.saveable {
        mutableStateOf(input.app)
    }

    var patchOptions: Options by savedStateHandle.saveable {
        mutableStateOf(emptyMap())
    }

    var selectionState by savedStateHandle.saveable {
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

    fun getCustomPatchesOrNull(
        bundles: List<BundleInfo>,
        allowUnsupported: Boolean
    ): PatchesSelection? =
        (selectionState as? SelectionState.Customized)?.patches(bundles, allowUnsupported)

    fun setNullablePatches(selection: PatchesSelection?) {
        selectionState = selection?.let(SelectionState::Customized) ?: SelectionState.Default
    }
}

sealed interface SelectionState : Parcelable {
    fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean): PatchesSelection

    /*
    @Parcelize
    data object Pending : SelectionState {
        override fun patches(
            bundles: List<BundleInfo>,
            allowUnsupported: Boolean
        ): PatchesSelection = emptyMap()
    }
     */

    @Parcelize
    data class Customized(val patchesSelection: PatchesSelection) : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean) =
            idkWhatToCallThis(
                bundles,
                allowUnsupported
            ) { uid, patch -> patchesSelection[uid]?.contains(patch.name) ?: false }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowUnsupported: Boolean) =
            idkWhatToCallThis(bundles, allowUnsupported) { _, patch -> patch.include }
    }
}

private inline fun idkWhatToCallThis(
    bundles: List<BundleInfo>,
    allowUnsupported: Boolean,
    condition: (Int, PatchInfo) -> Boolean
) = bundles.associate { bundle ->
    val patches =
        bundle.sequence(allowUnsupported)
            .mapNotNullTo(mutableSetOf()) { patch ->
                patch.name.takeIf {
                    condition(
                        bundle.uid,
                        patch
                    )
                }
            }

    bundle.uid to patches
}