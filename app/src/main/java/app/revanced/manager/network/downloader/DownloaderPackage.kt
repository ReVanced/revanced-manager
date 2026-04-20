package app.revanced.manager.network.downloader

import app.revanced.manager.domain.repository.DownloaderRepository

data class DownloaderPackage(
    val downloaders: List<LoadedDownloader>,
    val classLoader: ClassLoader,
    val resourceImpl: DownloaderRepository.ResourceImpl,
    val packageName: String,
    val name: String,
    val version: String
)