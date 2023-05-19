package app.revanced.manager.compose.ui.destination

import android.os.Parcelable
import app.revanced.manager.compose.util.PackageInfo
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {

    @Parcelize
    object Dashboard : Destination

    @Parcelize
    object AppSelector : Destination

    @Parcelize
    object Settings : Destination

    @Parcelize
    data class PatchesSelector(val input: PackageInfo) : Destination

    @Parcelize
    data class Installer(val input: PackageInfo, val selectedPatches: List<String>) : Destination
}