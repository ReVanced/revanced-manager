package app.revanced.manager.plugin.downloader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.net.HttpURLConnection
import java.net.URI

/**
 * A simple parcelable data class for storing a package name and version.
 * This can be used as the data type for plugins that only need a name and version to implement their [DownloaderScope.download] function.
 *
 * @param name The package name.
 * @param version The version.
 */
@Parcelize
data class Package(val name: String, val version: String) : Parcelable

/**
 * A data class for storing a download URL.
 *
 * @param url The download URL.
 * @param headers The headers to use for the request.
 */
@Parcelize
data class DownloadUrl(val url: String, val headers: Map<String, String> = emptyMap()) : Parcelable {
    /**
     * Converts this into a [DownloadResult].
     */
    fun toDownloadResult(): DownloadResult = with(URI.create(url).toURL().openConnection() as HttpURLConnection) {
        useCaches = false
        allowUserInteraction = false
        headers.forEach(::setRequestProperty)

        connectTimeout = 10_000
        connect()

        inputStream to getHeaderField("Content-Length").toLong()
    }
}