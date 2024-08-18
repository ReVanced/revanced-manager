package app.revanced.manager.network.downloader

import android.content.Context
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope
import app.revanced.manager.plugin.downloader.GetScope
import java.io.InputStream

class LoadedDownloaderPlugin(
    val packageName: String,
    val name: String,
    val version: String,
    val get: suspend GetScope.(packageName: String, version: String?) -> App?,
    val download: suspend DownloadScope.(app: App) -> InputStream,
    val classLoader: ClassLoader
)