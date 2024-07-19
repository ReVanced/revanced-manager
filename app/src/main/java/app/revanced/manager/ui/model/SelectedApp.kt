package app.revanced.manager.ui.model

import android.os.Parcelable
import app.revanced.manager.network.downloader.ParceledDownloaderApp
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File

sealed interface SelectedApp : Parcelable {
    val packageName: String
    val version: String // TODO: make this nullable

    @Parcelize
    data class Download(
        override val packageName: String,
        override val version: String,
        val app: ParceledDownloaderApp
    ) : SelectedApp

    @Parcelize
    data class Downloadable(override val packageName: String, val suggestedVersion: String?) : SelectedApp {
        @IgnoredOnParcel
        override val version = suggestedVersion.orEmpty()
    }

    @Parcelize
    data class Local(
        override val packageName: String,
        override val version: String,
        val file: File,
        val temporary: Boolean
    ) : SelectedApp

    @Parcelize
    data class Installed(
        override val packageName: String,
        override val version: String
    ) : SelectedApp
}
