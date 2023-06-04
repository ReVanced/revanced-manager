package app.revanced.manager.ui.destination

import android.os.Parcelable
import app.revanced.manager.util.AppInfo
import app.revanced.manager.util.PatchesSelection
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {

    @Parcelize
    object Dashboard : Destination

    @Parcelize
    object AppSelector : Destination

    @Parcelize
    object Settings : Destination

    @Parcelize
    data class PatchesSelector(val input: AppInfo) : Destination

    @Parcelize
    data class Installer(val input: AppInfo, val selectedPatches: PatchesSelection) : Destination
}