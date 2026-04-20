package app.revanced.manager.downloader

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.java

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is only intended for downloader hosts, don't use it in a downloader.",
)
@Retention(AnnotationRetention.BINARY)
annotation class DownloaderHostApi

/**
 * The base interface for all DSL scopes.
 */
interface Scope {
    /**
     * The package name of ReVanced Manager.
     */
    val hostPackageName: String

    /**
     * The package name of the downloader.
     */
    val downloaderPackageName: String

    /**
     * A data directory for this downloader package.
     */
    val dataDir: File
}

/**
 * The scope of [DownloaderScope.get].
 */
interface GetScope : Scope {
    /**
     * Ask the user to perform some required interaction in the activity specified by the provided [Intent].
     * This function returns normally with the resulting [Intent] when the activity finishes with code [Activity.RESULT_OK].
     *
     * @throws UserInteractionException.RequestDenied User decided to skip this downloader.
     * @throws UserInteractionException.Activity.Cancelled The activity was cancelled.
     * @throws UserInteractionException.Activity.NotCompleted The activity finished with an unknown result code.
     */
    suspend fun requestStartActivity(intent: Intent): Intent?

    /**
     * Starts an [Activity] using [requestStartActivity] which loads the specified [Fragment].
     * The fragment may reside in the downloader package.
     *
     * @param clazz The class of the fragment to launch.
     * @param args The fragment arguments.
     */
    suspend fun requestStartFragment(clazz: Class<out Fragment>, args: Bundle?) =
        requestStartActivity(Intent().apply {
            setClassName(hostPackageName, "app.revanced.manager.DownloaderActivity")
            // We shouldn't use the downloader's resources if it is launching a fragment that resides in manager itself.
            if (clazz.classLoader != GetScope::class.java.classLoader) putExtra(
                "DOWNLOADER_NAME",
                downloaderPackageName
            )
            putExtra("FRAGMENT_CLASS_NAME", clazz.name)
            putExtra("FRAGMENT_ARGS", args)
        })
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
    internal val context: Context,
    internal val resources: Resources
) : Scope by scopeImpl {
    // Returning an InputStream is the primary way for a downloader to implement the download function, but we also want to offer an OutputStream API since using InputStream might not be convenient in all cases.
    // It is much easier to implement the main InputStream API on top of OutputStreams compared to doing it the other way around, which is why we are using OutputStream here.
    internal var download: (suspend OutputDownloadScope.(T, OutputStream) -> Unit)? = null
    internal var get: (suspend GetScope.(String, String?) -> GetResult<T>?)? = null
    private val inputDownloadScopeImpl = object : InputDownloadScope, Scope by scopeImpl {}

    /**
     * Define the download block of the downloader.
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
     * Define the get block of the downloader.
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

class DownloaderBuilder<T : Parcelable> internal constructor(
    @param:StringRes private val name: Int,
    private val block: DownloaderScope<T>.() -> Unit
) {
    @DownloaderHostApi
    fun build(scopeImpl: Scope, context: Context, resources: Resources) =
        with(DownloaderScope<T>(scopeImpl, context, resources)) {
            block()

            Downloader(
                download = download!!,
                get = get!!,
                name = name,
            )
        }
}

class Downloader<T : Parcelable> internal constructor(
    @property:DownloaderHostApi val get: suspend GetScope.(packageName: String, version: String?) -> GetResult<T>?,
    @property:DownloaderHostApi val download: suspend OutputDownloadScope.(data: T, outputStream: OutputStream) -> Unit,
    @property:DownloaderHostApi @param:StringRes val name: Int,
)

/**
 * Define a downloader.
 */
fun <T : Parcelable> Downloader(@StringRes name: Int, block: DownloaderScope<T>.() -> Unit) =
    DownloaderBuilder(name, block)

/**
 * @see GetScope.requestStartActivity
 */
sealed class UserInteractionException(message: String) : Exception(message) {
    class RequestDenied @DownloaderHostApi constructor() :
        UserInteractionException("Request denied by user")

    sealed class Activity(message: String) : UserInteractionException(message) {
        class Cancelled @DownloaderHostApi constructor() : Activity("Interaction cancelled")

        /**
         * @param resultCode The result code of the activity.
         * @param intent The [Intent] of the activity.
         */
        class NotCompleted @DownloaderHostApi constructor(
            val resultCode: Int,
            val intent: Intent?
        ) :
            Activity("Unexpected activity result code: $resultCode")
    }
}