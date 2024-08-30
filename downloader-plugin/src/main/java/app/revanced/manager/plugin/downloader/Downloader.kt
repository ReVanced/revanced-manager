package app.revanced.manager.plugin.downloader

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcelable
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is only intended for plugin hosts, don't use it in a plugin.",
)
annotation class PluginHostApi

interface GetScope {
    suspend fun requestStartActivity(intent: Intent): Intent?
}

typealias Size = Long
typealias DownloadResult = Pair<InputStream, Size?>

typealias Version = String
typealias GetResult<T> = Pair<T, Version?>

class DownloaderScope<T : Parcelable> internal constructor(
    /**
     * The package name of ReVanced Manager.
     */
    val hostPackageName: String,
    internal val context: Context
) {
    internal var download: (suspend DownloadScope.(T, OutputStream) -> Unit)? = null
    internal var get: (suspend GetScope.(String, String?) -> GetResult<T>?)? = null

    /**
     * The package name of the plugin.
     */
    val pluginPackageName: String get() = context.packageName

    fun get(block: suspend GetScope.(packageName: String, version: String?) -> GetResult<T>?) {
        get = block
    }

    /**
     * Define the download function for this plugin.
     */
    fun download(block: suspend (data: T) -> DownloadResult) {
        download = { app, outputStream ->
            val (inputStream, size) = block(app)

            inputStream.use {
                if (size != null) reportSize(size)
                it.copyTo(outputStream)
            }
        }
    }

    suspend fun <R : Any?> withBoundService(intent: Intent, block: suspend (IBinder) -> R): R {
        var onBind: ((IBinder) -> Unit)? = null
        val serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) =
                onBind!!(service!!)

            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        return try {
            // TODO: add a timeout
            block(suspendCoroutine { continuation ->
                onBind = continuation::resume
                context.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
            })
        } finally {
            onBind = null
            // TODO: should we stop it?
            context.unbindService(serviceConn)
        }
    }
}

class DownloaderBuilder<T : Parcelable> internal constructor(private val block: DownloaderScope<T>.() -> Unit) {
    @PluginHostApi
    fun build(hostPackageName: String, context: Context) =
        with(DownloaderScope<T>(hostPackageName, context)) {
            block()

            Downloader(
                download = download ?: error("download was not declared"),
                get = get ?: error("get was not declared")
            )
        }
}

class Downloader<T : Parcelable> internal constructor(
    @property:PluginHostApi val get: suspend GetScope.(packageName: String, version: String?) -> GetResult<T>?,
    @property:PluginHostApi val download: suspend DownloadScope.(data: T, outputStream: OutputStream) -> Unit
)

fun <T : Parcelable> downloader(block: DownloaderScope<T>.() -> Unit) = DownloaderBuilder(block)

sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied @PluginHostApi constructor() :
        UserInteractionException("Request was denied")

    sealed class Activity(message: String) : UserInteractionException(message) {
        class Cancelled @PluginHostApi constructor() : Activity("Interaction was cancelled")
        class NotCompleted @PluginHostApi constructor(val resultCode: Int, val intent: Intent?) :
            Activity("Unexpected activity result code: $resultCode")
    }
}