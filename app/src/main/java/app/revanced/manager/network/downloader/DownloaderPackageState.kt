package app.revanced.manager.network.downloader

import android.content.Context

sealed interface DownloaderPackageState {
    data object Untrusted : DownloaderPackageState

    data class Loaded(
        val downloaders: List<LoadedDownloader>,
        val classLoader: ClassLoader,
        val context: Context,
        val name: String
    ) : DownloaderPackageState

    data class Failed(val throwable: Throwable) : DownloaderPackageState
}