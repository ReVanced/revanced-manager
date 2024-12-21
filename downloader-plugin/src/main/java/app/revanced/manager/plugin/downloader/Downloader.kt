package app.revanced.manager.plugin.downloader

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.app.Activity
import android.os.Parcelable
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is only intended for plugin hosts, don't use it in a plugin.",
)
@Retention(AnnotationRetention.BINARY)
annotation class PluginHostApi

/**
 * The base interface for all DSL scopes.
 */
interface Scope {
    /**
     * The package name of ReVanced Manager.
     */
    val hostPackageName: String

    /**
     * The package name of the plugin.
     */
    val pluginPackageName: String
}

/**
 * The scope of [DownloaderScope.get].
 */
interface GetScope : Scope {
    /**
     * Ask the user to perform some required interaction in the activity specified by the provided [Intent].
     * This function returns normally with the resulting [Intent] when the activity finishes with code [Activity.RESULT_OK].
     *
     * @throws UserInteractionException.RequestDenied User decided to skip this plugin.
     * @throws UserInteractionException.Activity.Cancelled The activity was cancelled.
     * @throws UserInteractionException.Activity.NotCompleted The activity finished with an unknown result code.
     */
    suspend fun requestStartActivity(intent: Intent): Intent?
}

interface BaseDownloadScope : Scope

/**
 * The scope for [DownloaderScope.download].
 */
interface InputDownloadScope : BaseDownloadScope

typealias Size = Long
typealias DownloadResult = Pair<InputStream, Size?>

typealias Version = String
typealias GetResult<T> = Pair<T, Version?>

class DownloaderScope<T : Parcelable> internal constructor(
    private val scopeImpl: Scope,
    internal val context: Context
) : Scope by scopeImpl {
    // Returning an InputStream is the primary way for plugins to implement the download function, but we also want to offer an OutputStream API since using InputStream might not be convenient in all cases.
    // It is much easier to implement the main InputStream API on top of OutputStreams compared to doing it the other way around, which is why we are using OutputStream here. This detail is not visible to plugins.
    internal var download: (suspend OutputDownloadScope.(T, OutputStream) -> Unit)? = null
    internal var get: (suspend GetScope.(String, String?) -> GetResult<T>?)? = null
    private val inputDownloadScopeImpl = object : InputDownloadScope, Scope by scopeImpl {}

    /**
     * Define the download block of the plugin.
     */
    fun download(block: suspend InputDownloadScope.(data: T) -> DownloadResult) {
        download = { app, outputStream ->
            val (inputStream, size) = inputDownloadScopeImpl.block(app)

            inputStream.use {
                if (size != null) reportSize(size)
                it.copyTo(outputStream)
            }
        }
    }

    /**
     * Define the get block of the plugin.
     * The block should return null if the app cannot be found. The version in the result must match the version argument unless it is null.
     */
    fun get(block: suspend GetScope.(packageName: String, version: String?) -> GetResult<T>?) {
        get = block
    }

    /**
     * Utilize the service specified by the provided [Intent]. The service will be unbound when the scope ends.
     */
    suspend fun <R : Any?> useService(intent: Intent, block: suspend (IBinder) -> R): R {
        var onBind: ((IBinder) -> Unit)? = null
        val serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) =
                onBind!!(service!!)

            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        return try {
            val binder = withTimeout(10000L) {
                suspendCoroutine { continuation ->
                    onBind = continuation::resume
                    context.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
                }
            }
            block(binder)
        } finally {
            onBind = null
            context.unbindService(serviceConn)
        }
    }
}

class DownloaderBuilder<T : Parcelable> internal constructor(private val block: DownloaderScope<T>.() -> Unit) {
    @PluginHostApi
    fun build(scopeImpl: Scope, context: Context) =
        with(DownloaderScope<T>(scopeImpl, context)) {
            block()

            Downloader(
                download = download!!,
                get = get!!
            )
        }
}

class Downloader<T : Parcelable> internal constructor(
    @property:PluginHostApi val get: suspend GetScope.(packageName: String, version: String?) -> GetResult<T>?,
    @property:PluginHostApi val download: suspend OutputDownloadScope.(data: T, outputStream: OutputStream) -> Unit
)

/**
 * Define a downloader plugin.
 */
fun <T : Parcelable> Downloader(block: DownloaderScope<T>.() -> Unit) = DownloaderBuilder(block)

/**
 * @see GetScope.requestStartActivity
 */
sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied @PluginHostApi constructor() :
        UserInteractionException("Request denied by user")

    sealed class Activity(message: String) : UserInteractionException(message) {
        class Cancelled @PluginHostApi constructor() : Activity("Interaction cancelled")

        /**
         * @param resultCode The result code of the activity.
         * @param intent The [Intent] of the activity.
         */
        class NotCompleted @PluginHostApi constructor(val resultCode: Int, val intent: Intent?) :
            Activity("Unexpected activity result code: $resultCode")
    }
}