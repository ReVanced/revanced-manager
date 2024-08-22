package app.revanced.manager.plugin.downloader

import android.content.Context
import android.content.Intent
import java.io.InputStream
import java.io.OutputStream

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is only intended for plugin hosts, don't use it in a plugin.",
)
annotation class PluginHostApi

interface GetScope {
    suspend fun requestUserInteraction(): ActivityLaunchPermit
}

fun interface ActivityLaunchPermit {
    suspend fun launch(intent: Intent): Intent?
}

interface DownloadScope {
    suspend fun reportSize(size: Long)
}

typealias Size = Long
typealias DownloadResult = Pair<InputStream, Size?>

class DownloaderScope<A : App> internal constructor(
    /**
     * The package name of ReVanced Manager.
     */
    val hostPackageName: String,
    internal val context: Context
) {
    internal var download: (suspend DownloadScope.(A, OutputStream) -> Unit)? = null
    internal var get: (suspend GetScope.(String, String?) -> A?)? = null

    /**
     * The package name of the plugin.
     */
    val pluginPackageName: String get() = context.packageName

    fun get(block: suspend GetScope.(packageName: String, version: String?) -> A?) {
        get = block
    }

    /**
     * Define the download function for this plugin.
     */
    fun download(block: suspend (app: A) -> DownloadResult) {
        download = { app, outputStream ->
            val (inputStream, size) = block(app)

            inputStream.use {
                if (size != null) reportSize(size)
                it.copyTo(outputStream)
            }
        }
    }
}

class DownloaderBuilder<A : App> internal constructor(private val block: DownloaderScope<A>.() -> Unit) {
    @PluginHostApi
    fun build(hostPackageName: String, context: Context) =
        with(DownloaderScope<A>(hostPackageName, context)) {
            block()

            Downloader(
                download = download ?: error("download was not declared"),
                get = get ?: error("get was not declared")
            )
        }
}

class Downloader<A : App> internal constructor(
    @property:PluginHostApi val get: suspend GetScope.(packageName: String, version: String?) -> A?,
    @property:PluginHostApi val download: suspend DownloadScope.(app: A, outputStream: OutputStream) -> Unit
)

fun <A : App> downloader(block: DownloaderScope<A>.() -> Unit) = DownloaderBuilder(block)

sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied : UserInteractionException("Request was denied")

    sealed class Activity(message: String) : UserInteractionException(message) {
        class Cancelled : Activity("Interaction was cancelled")
        class NotCompleted(val resultCode: Int, val intent: Intent?) :
            Activity("Unexpected activity result code: $resultCode")
    }
}