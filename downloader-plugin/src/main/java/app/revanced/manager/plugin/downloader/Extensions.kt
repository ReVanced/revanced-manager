package app.revanced.manager.plugin.downloader

import java.io.OutputStream

// OutputStream-based version of download
fun <A : App> DownloaderScope<A>.download(block: suspend DownloadScope.(A, OutputStream) -> Unit) {
    download = block
}