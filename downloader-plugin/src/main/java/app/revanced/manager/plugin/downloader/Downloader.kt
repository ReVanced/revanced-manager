package app.revanced.manager.plugin.downloader

import android.content.Intent
import java.io.File

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class DownloaderDsl

@DownloaderDsl
interface GetScope {
    suspend fun requestUserInteraction(): ActivityLaunchPermit
}

fun interface ActivityLaunchPermit {
    suspend fun launch(intent: Intent): Intent?
}

@DownloaderDsl
interface DownloadScope {
    /**
     * The location where the downloaded APK should be saved.
     */
    val targetFile: File

    /**
     * A callback for reporting download progress
     */
    suspend fun reportProgress(bytesReceived: Int, bytesTotal: Int?)
}

@DownloaderDsl
class DownloaderBuilder<A : App> {
    private var download: (suspend DownloadScope.(A) -> Unit)? = null
    private var get: (suspend GetScope.(String, String?) -> A?)? = null

    fun get(block: suspend GetScope.(packageName: String, version: String?) -> A?) {
        get = block
    }

    fun download(block: suspend DownloadScope.(app: A) -> Unit) {
        download = block
    }

    fun build() = Downloader(
        download = download ?: error("download was not declared"),
        get = get ?: error("get was not declared")
    )
}

class Downloader<A : App> internal constructor(
    val get: suspend GetScope.(packageName: String, version: String?) -> A?,
    val download: suspend DownloadScope.(app: A) -> Unit
)

fun <A : App> downloader(block: DownloaderBuilder<A>.() -> Unit) =
    DownloaderBuilder<A>().apply(block).build()

sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied : UserInteractionException("Request was denied")
    // TODO: can cancelled activities return an intent?
    class ActivityCancelled : UserInteractionException("Interaction was cancelled")
}