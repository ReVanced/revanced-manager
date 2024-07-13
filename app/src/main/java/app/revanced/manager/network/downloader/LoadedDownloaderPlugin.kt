package app.revanced.manager.network.downloader

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope

class LoadedDownloaderPlugin(
    val packageName: String,
    val name: String,
    val version: String,
    val createVersionPagingSource: (packageName: String, versionHint: String?) -> PagingSource<*, out App>,
    val download: suspend DownloadScope.(app: App) -> Unit,
    val pagingConfig: PagingConfig,
    val classLoader: ClassLoader
)