package app.revanced.manager.network.downloader

import android.os.Parcelable
import app.revanced.manager.plugin.downloader.DownloadScope
import app.revanced.manager.plugin.downloader.GetScope
import java.io.OutputStream

class LoadedDownloaderPlugin(
    val packageName: String,
    val name: String,
    val version: String,
    val get: suspend GetScope.(packageName: String, version: String?) -> Pair<Parcelable, String?>?,
    val download: suspend DownloadScope.(data: Parcelable, outputStream: OutputStream) -> Unit,
    val classLoader: ClassLoader
)