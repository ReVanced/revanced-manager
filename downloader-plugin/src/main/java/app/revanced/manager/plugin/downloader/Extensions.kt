package app.revanced.manager.plugin.downloader

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import java.io.OutputStream

interface DownloadScope {
    suspend fun reportSize(size: Long)
}

// OutputStream-based version of download
fun <T : Parcelable> DownloaderScope<T>.download(block: suspend DownloadScope.(T, OutputStream) -> Unit) {
    download = block
}

suspend inline fun <reified ACTIVITY : Activity> GetScope.requestStartActivity(packageName: String) =
    requestStartActivity(
        Intent().apply { setClassName(packageName, ACTIVITY::class.qualifiedName!!) }
    )

suspend inline fun <reified SERVICE : Service, R : Any?> DownloaderScope<*>.withBoundService(
    packageName: String,
    noinline block: suspend (IBinder) -> R
) = withBoundService(
    Intent().apply { setClassName(packageName, SERVICE::class.qualifiedName!!) }, block
)