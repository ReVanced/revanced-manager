package app.revanced.manager.ui.destination

import android.os.Parcelable
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

sealed interface SelectedAppInfoDestination : Parcelable {
    @Parcelize
    data object Main : SelectedAppInfoDestination

    @Parcelize
    data class PatchesSelector(val app: SelectedApp, val currentSelection: PatchesSelection?, val options: @RawValue Options) : SelectedAppInfoDestination
}