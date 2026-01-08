package app.revanced.manager.network.downloader

sealed interface DownloaderPackageState {
    data object Untrusted : DownloaderPackageState

    data class Loaded(val downloader: List<LoadedDownloader>) : DownloaderPackageState

    data class Failed(val throwable: Throwable) : DownloaderPackageState
}