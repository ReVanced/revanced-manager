package app.revanced.manager.plugin.downloader

import android.content.Context
import android.os.Parcelable
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import java.io.File

@Suppress("Unused")
/**
 * The main interface for downloader plugins.
 * Implementors must have a public constructor that takes exactly one argument of type [DownloaderPlugin.Parameters].
 */
interface DownloaderPlugin<A : DownloaderPlugin.App> {
    val pagingConfig: PagingConfig
    fun createPagingSource(parameters: SearchParameters): PagingSource<*, A>
    suspend fun download(app: A, parameters: DownloadParameters)

    interface App : Parcelable {
        val packageName: String
        val version: String
    }

    /**
     * The plugin constructor parameters.
     *
     * @param context An Android [Context].
     * @param tempDirectory The temporary directory belonging to this [DownloaderPlugin].
     */
    class Parameters(val context: Context, val tempDirectory: File)

    /**
     * The application pager parameters.
     *
     * @param packageName The package name to search for.
     * @param versionHint The preferred version to search for. It is not mandatory to respect this parameter.
     */
    class SearchParameters(val packageName: String, val versionHint: String?)

    /**
     * The parameters for downloading apps.
     *
     * @param targetFile The location where the downloaded APK should be saved.
     * @param onDownloadProgress A callback for reporting download progress.
     */
    class DownloadParameters(
        val targetFile: File,
        val onDownloadProgress: suspend (progress: Pair<BytesReceived, BytesTotal>?) -> Unit
    )
}

typealias BytesReceived = Int
typealias BytesTotal = Int