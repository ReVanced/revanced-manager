package app.revanced.manager.network.downloader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AppDownloader {
    val downloadProgress: StateFlow<Pair<Float, Float>?>

    /**
     * Returns all downloadable apps.
     *
     * @param packageName The package name of the app.
     * @param versionFilter A set of versions to filter.
     */
    fun getAvailableVersions(packageName: String, versionFilter: Set<String>): Flow<String>

    /**
     * Downloads the specific app version.
     *
     * @param version The version to download.
     * @param saveDirectory The folder where the downloaded app should be stored.
     * @param preferSplit Whether it should prefer a split or a full apk.
     * @return the downloaded apk or the folder containing all split apks.
     */
    suspend fun downloadApp(
        version: String,
        saveDirectory: File,
        preferSplit: Boolean = false
    ): File
}