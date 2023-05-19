package app.revanced.manager.compose.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsDestination : Parcelable {

    @Parcelize
    object Settings : SettingsDestination

    @Parcelize
    object General : SettingsDestination

    @Parcelize
    object Updates : SettingsDestination

    @Parcelize
    object Downloads : SettingsDestination

    @Parcelize
    object ImportExport : SettingsDestination

    @Parcelize
    object About : SettingsDestination

}