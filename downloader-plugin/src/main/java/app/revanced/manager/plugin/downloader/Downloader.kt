package app.revanced.manager.plugin.downloader

class Downloader<A : App> internal constructor(
    val getVersions: suspend (packageName: String, versionHint: String?) -> List<A>,
    val download: suspend DownloadScope.(app: A) -> Unit
) : DownloaderMarker

class DownloaderBuilder<A : App> {
    private var getVersions: (suspend (String, String?) -> List<A>)? = null
    private var download: (suspend DownloadScope.(A) -> Unit)? = null

    fun getVersions(block: suspend (packageName: String, versionHint: String?) -> List<A>) {
        getVersions = block
    }

    fun download(block: suspend DownloadScope.(app: A) -> Unit) {
        download = block
    }

    fun build() = Downloader(
        getVersions = getVersions ?: error("getVersions was not declared"),
        download = download ?: error("download was not declared")
    )
}

fun <A : App> downloader(block: DownloaderBuilder<A>.() -> Unit) =
    DownloaderBuilder<A>().apply(block).build()