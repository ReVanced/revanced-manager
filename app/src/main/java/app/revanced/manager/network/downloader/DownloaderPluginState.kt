package app.revanced.manager.network.downloader

sealed interface DownloaderPluginState {
    data object Untrusted : DownloaderPluginState

    data class Loaded(val plugin: LoadedDownloaderPlugin) : DownloaderPluginState

    data class Failed(val throwable: Throwable) : DownloaderPluginState
}