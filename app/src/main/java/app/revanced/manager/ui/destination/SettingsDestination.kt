package app.revanced.manager.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsDestination : Parcelable {

    @Parcelize
    data object Settings : SettingsDestination

    @Parcelize
    data object General : SettingsDestination

    @Parcelize
    data object Advanced : SettingsDestination

    @Parcelize
    data object Updates : SettingsDestination

    @Parcelize
    data object Downloads : SettingsDestination

    @Parcelize
    data object ImportExport : SettingsDestination

    @Parcelize
    data object About : SettingsDestination

    @Parcelize
    data class Update(val downloadOnScreenEntry: Boolean = false) : SettingsDestination

    @Parcelize
    data object Changelogs : SettingsDestination

    @Parcelize
    data object Contributors: SettingsDestination

    @Parcelize
    data object Licenses: SettingsDestination

    @Parcelize
    data object DeveloperOptions: SettingsDestination
}