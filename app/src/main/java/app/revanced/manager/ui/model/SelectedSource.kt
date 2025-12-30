package app.revanced.manager.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SelectedSource : Parcelable {
    data object Auto : SelectedSource()
    data object Installed : SelectedSource()
    data class Downloaded(val path: String) : SelectedSource()
    data class Plugin(val plugin: String) : SelectedSource() // TODO
}