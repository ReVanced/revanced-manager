package app.revanced.manager.plugin.downloader

import android.content.Intent
import java.io.File
import java.io.InputStream

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
    suspend fun reportSize(size: Long)
}

@DownloaderDsl
class DownloaderBuilder<A : App> {
    private var download: (suspend DownloadScope.(A) -> InputStream)? = null
    private var get: (suspend GetScope.(String, String?) -> A?)? = null

    fun get(block: suspend GetScope.(packageName: String, version: String?) -> A?) {
        get = block
    }

    fun download(block: suspend DownloadScope.(app: A) -> InputStream) {
        download = block
    }

    fun build() = Downloader(
        download = download ?: error("download was not declared"),
        get = get ?: error("get was not declared")
    )
}

class Downloader<A : App> internal constructor(
    val get: suspend GetScope.(packageName: String, version: String?) -> A?,
    val download: suspend DownloadScope.(app: A) -> InputStream
)

fun <A : App> downloader(block: DownloaderBuilder<A>.() -> Unit) =
    DownloaderBuilder<A>().apply(block).build()

sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied : UserInteractionException("Request was denied")

    sealed class Activity(message: String) : UserInteractionException(message) {
        class Cancelled : Activity("Interaction was cancelled")
        class NotCompleted(val resultCode: Int, val intent: Intent?) :
            Activity("Unexpected activity result code: $resultCode")
    }
}