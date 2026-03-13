package app.revanced.manager.network.downloader

import android.content.Context

data class DownloaderPackage(
    val downloaders: List<LoadedDownloader>,
    val classLoader: ClassLoader,
    val context: Context,
    val name: String
)