package app.revanced.manager.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SelectedSource : Parcelable {
    data object Auto : SelectedSource()
    data object Installed : SelectedSource()
    data class Downloaded(val path: String, val version: String) : SelectedSource()
    data class Local(val path: String) : SelectedSource()
    data class Downloader(
        val packageName: String? = null,
        val className: String? = null
    ) : SelectedSource()
}
