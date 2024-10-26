package app.revanced.manager.plugin.downloader

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import java.io.OutputStream

/**
 * The scope of [DownloaderScope.download].
 */
interface DownloadScope : Scope {
    suspend fun reportSize(size: Long)
}

// OutputStream-based version of download
fun <T : Parcelable> DownloaderScope<T>.download(block: suspend DownloadScope.(T, OutputStream) -> Unit) {
    download = block
}

/**
 * Performs [GetScope.requestStartActivity] with an [Intent] created using the type information of [ACTIVITY].
 * @see [GetScope.requestStartActivity]
 */
suspend inline fun <reified ACTIVITY : Activity> GetScope.requestStartActivity() =
    requestStartActivity(
        Intent().apply { setClassName(pluginPackageName, ACTIVITY::class.qualifiedName!!) }
    )

/**
 * Performs [DownloaderScope.withBoundService] with an [Intent] created using the type information of [SERVICE].
 * @see [DownloaderScope.withBoundService]
 */
suspend inline fun <reified SERVICE : Service, R : Any?> DownloaderScope<*>.withBoundService(
    noinline block: suspend (IBinder) -> R
) = withBoundService(
    Intent().apply { setClassName(pluginPackageName, SERVICE::class.qualifiedName!!) }, block
)