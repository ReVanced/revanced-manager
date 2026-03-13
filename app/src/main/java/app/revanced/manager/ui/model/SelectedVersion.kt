package app.revanced.manager.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SelectedVersion : Parcelable {
    data object Auto : SelectedVersion()
    data object Any : SelectedVersion()
    data class Specific(val version: String) : SelectedVersion()
}