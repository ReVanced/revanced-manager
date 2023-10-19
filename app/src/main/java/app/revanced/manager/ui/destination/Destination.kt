package app.revanced.manager.ui.destination

import android.os.Parcelable
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

sealed interface Destination : Parcelable {

    @Parcelize
    object Dashboard : Destination

    @Parcelize
    data class InstalledApplicationInfo(val installedApp: InstalledApp) : Destination

    @Parcelize
    object AppSelector : Destination

    @Parcelize
    object Settings : Destination

    @Parcelize
    data class VersionSelector(val packageName: String, val patchesSelection: PatchesSelection? = null) : Destination

    @Parcelize
    data class SelectedApplicationInfo(val selectedApp: SelectedApp, val patchesSelection: PatchesSelection? = null) : Destination

    @Parcelize
    data class Installer(val selectedApp: SelectedApp, val selectedPatches: PatchesSelection, val options: @RawValue Options) : Destination

}