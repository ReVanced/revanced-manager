package app.revanced.manager.network.downloader

import android.os.Parcelable
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AppDownloader {

    /**
     * Returns all downloadable apps.
     *
     * @param packageName The package name of the app.
     * @param versionFilter A set of versions to filter.
     */
    fun getAvailableVersions(packageName: String, versionFilter: Set<String>): Flow<App>

    interface App : Parcelable {
        val packageName: String
        val version: String

        suspend fun download(
            saveDirectory: File,
            preferSplit: Boolean,
            onDownload: suspend (downloadProgress: Pair<Float, Float>?) -> Unit = {}
        ): File
    }

}