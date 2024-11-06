package app.revanced.manager.ui.destination

import android.os.Parcelable
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

sealed interface Destination : Parcelable {

    @Parcelize
    data object Dashboard : Destination

    @Parcelize
    data class InstalledApplicationInfo(val installedApp: InstalledApp) : Destination

    @Parcelize
    data object AppSelector : Destination

    @Parcelize
    data class Settings(val startDestination: SettingsDestination = SettingsDestination.Settings) : Destination

    @Parcelize
    data class VersionSelector(val packageName: String, val patchSelection: PatchSelection? = null) : Destination

    @Parcelize
    data class SelectedApplicationInfo(val selectedApp: SelectedApp, val patchSelection: PatchSelection? = null) : Destination

    @Parcelize
    data class Patcher(val selectedApp: SelectedApp, val selectedPatches: PatchSelection, val options: @RawValue Options) : Destination

}