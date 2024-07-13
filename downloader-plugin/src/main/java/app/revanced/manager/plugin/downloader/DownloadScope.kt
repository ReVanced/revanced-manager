package app.revanced.manager.plugin.downloader

import java.io.File

interface DownloadScope {
    /**
     * The location where the downloaded APK should be saved.
     */
    val saveLocation: File

    /**
     * A callback for reporting download progress
     */
    suspend fun reportProgress(bytesReceived: Int, bytesTotal: Int?)
}