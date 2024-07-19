package app.revanced.manager.network.downloader

import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope
import app.revanced.manager.plugin.downloader.GetScope

class LoadedDownloaderPlugin(
    val packageName: String,
    val name: String,
    val version: String,
    val get: suspend GetScope.(packageName: String, version: String?) -> App?,
    val download: suspend DownloadScope.(app: App) -> Unit,
    val classLoader: ClassLoader
)