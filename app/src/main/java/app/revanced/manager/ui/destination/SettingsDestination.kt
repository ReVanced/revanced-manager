package app.revanced.manager.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsDestination : Parcelable {

    @Parcelize
    object Settings : SettingsDestination

    @Parcelize
    object General : SettingsDestination

    @Parcelize
    object Advanced : SettingsDestination

    @Parcelize
    object Updates : SettingsDestination

    @Parcelize
    object Downloads : SettingsDestination

    @Parcelize
    object ImportExport : SettingsDestination

    @Parcelize
    object About : SettingsDestination

    @Parcelize
    object UpdateProgress : SettingsDestination

    @Parcelize
    object UpdateChangelog : SettingsDestination

    @Parcelize
    object Contributors: SettingsDestination

    @Parcelize
    object Licenses: SettingsDestination

}