package app.revanced.manager.network.downloader

import app.revanced.manager.plugin.downloader.DownloaderPlugin

class LoadedDownloaderPlugin(
    val packageName: String,
    val name: String,
    val version: String,
    private val instance: DownloaderPlugin<DownloaderPlugin.App>,
    val classLoader: ClassLoader
) : DownloaderPlugin<DownloaderPlugin.App> by instance