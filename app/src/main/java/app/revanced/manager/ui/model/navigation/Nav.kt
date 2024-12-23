package app.revanced.manager.ui.model.navigation

import android.os.Parcelable
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable

/*
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
    data class SelectedApplicationInfo(val selectedApp: SelectedApp, val patchSelection: PatchSelection? = null) : Destination

    @Parcelize
    data class Patcher(val selectedApp: SelectedApp, val selectedPatches: PatchSelection, val options: @RawValue Options) : Destination

}*/

interface ComplexParameter<T : Parcelable>

@Serializable
object Dashboard

@Serializable
object AppSelector

@Serializable
data class InstalledApplicationInfo(val packageName: String)

@Serializable
data class Update(val downloadOnScreenEntry: Boolean = false)

@Serializable
data object SelectedApplicationInfo : ComplexParameter<SelectedApplicationInfo.ViewModelParams> {
    @Parcelize
    data class ViewModelParams(
        val app: SelectedApp,
        val patches: PatchSelection? = null
    ) : Parcelable

    @Serializable
    object Main

    @Serializable
    data object PatchesSelector : ComplexParameter<PatchesSelector.ViewModelParams> {
        @Parcelize
        data class ViewModelParams(
            val app: SelectedApp,
            val currentSelection: PatchSelection?,
            val options: @RawValue Options,
        ) : Parcelable
    }
}

@Serializable
data object Patcher : ComplexParameter<Patcher.ViewModelParams> {
    @Parcelize
    data class ViewModelParams(
        val selectedApp: SelectedApp,
        val selectedPatches: PatchSelection,
        val options: @RawValue Options
    ) : Parcelable
}

@Serializable
object Settings {
    sealed interface Destination

    @Serializable
    data object Main : Destination

    @Serializable
    data object General : Destination

    @Serializable
    data object Advanced : Destination

    @Serializable
    data object Updates : Destination

    @Serializable
    data object Downloads : Destination

    @Serializable
    data object ImportExport : Destination

    @Serializable
    data object About : Destination

    @Serializable
    data object Changelogs : Destination

    @Serializable
    data object Contributors : Destination

    @Serializable
    data object Licenses : Destination

    @Serializable
    data object DeveloperOptions : Destination
}