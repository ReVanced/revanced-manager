package app.revanced.manager.ui.model

import android.os.Parcelable
import app.revanced.manager.network.downloader.AppDownloader
import kotlinx.parcelize.Parcelize
import java.io.File

sealed class SelectedApp : Parcelable {
    abstract val packageName: String
    abstract val version: String

    @Parcelize
    data class Download(override val packageName: String, override val version: String, val app: AppDownloader.App) : SelectedApp()

    @Parcelize
    data class Local(override val packageName: String, override val version: String, val file: File) : SelectedApp()

    @Parcelize
    data class Installed(override val packageName: String, override val version: String) : SelectedApp()
}
