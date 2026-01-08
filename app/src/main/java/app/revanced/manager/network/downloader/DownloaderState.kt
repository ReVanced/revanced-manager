package app.revanced.manager.network.downloader

sealed interface DownloaderState {
    data object Untrusted : DownloaderState

    data class Loaded(val downloader: LoadedDownloader) : DownloaderState

    data class Failed(val throwable: Throwable) : DownloaderState
}