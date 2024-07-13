package app.revanced.manager.plugin.downloader

import androidx.paging.PagingConfig
import androidx.paging.PagingSource

class PaginatedDownloader<A : App> internal constructor(
    val versionPager: (packageName: String, versionHint: String?) -> PagingSource<*, A>,
    val pagingConfig: PagingConfig,
    val download: suspend DownloadScope.(app: A) -> Unit
) : DownloaderMarker

class PaginatedDownloaderBuilder<A : App> {
    private var versionPager: ((String, String?) -> PagingSource<*, A>)? = null
    private var download: (suspend DownloadScope.(A) -> Unit)? = null
    private var pagingConfig: PagingConfig? = null

    fun versionPager(
        pagingConfig: PagingConfig = PagingConfig(pageSize = 5),
        block: (packageName: String, versionHint: String?) -> PagingSource<*, A>
    ) {
        versionPager = block
        this.pagingConfig = pagingConfig
    }

    fun download(block: suspend DownloadScope.(app: A) -> Unit) {
        download = block
    }

    fun build() = PaginatedDownloader(
        versionPager = versionPager ?: error("versionPager was not declared"),
        download = download ?: error("download was not declared"),
        pagingConfig = pagingConfig!!
    )
}

fun <A : App> paginatedDownloader(block: PaginatedDownloaderBuilder<A>.() -> Unit) =
    PaginatedDownloaderBuilder<A>().apply(block).build()