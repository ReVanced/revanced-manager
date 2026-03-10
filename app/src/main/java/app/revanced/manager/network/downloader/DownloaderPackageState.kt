package app.revanced.manager.network.downloader

sealed interface DownloaderPackageState {
    data object Untrusted : DownloaderPackageState

    data class Loaded(
        val downloaders: List<LoadedDownloader>,
        val classLoader: ClassLoader,
        val name: String
    ) : DownloaderPackageState

    data class Failed(val throwable: Throwable) : DownloaderPackageState
}