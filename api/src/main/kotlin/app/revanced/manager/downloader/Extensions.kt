package app.revanced.manager.downloader

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.OutputStream

/**
 * The scope of the [OutputStream] version of [DownloaderScope.download].
 */
interface OutputDownloadScope : BaseDownloadScope {
    suspend fun reportSize(size: Long)
}

/**
 * A replacement for [DownloaderScope.download] that uses [OutputStream].
 * The provided [OutputStream] does not need to be closed manually.
 */
fun <T : Parcelable> DownloaderScope<T>.download(block: suspend OutputDownloadScope.(T, OutputStream) -> Unit) {
    download = block
}

/**
 * Performs [GetScope.requestStartActivity] with an [Intent] created using the type information of [ACTIVITY].
 * @see [GetScope.requestStartActivity]
 */
suspend inline fun <reified ACTIVITY : Activity> GetScope.requestStartActivity() =
    requestStartActivity(
        Intent().apply { setClassName(downloaderPackageName, ACTIVITY::class.qualifiedName!!) }
    )

/**
 * Starts an [Activity] using [GetScope.requestStartActivity] which loads the specified [Fragment].
 * The fragment may reside in the downloader package.
 *
 * @param args The fragment arguments.
 */
suspend inline fun <reified T : Fragment> GetScope.requestStartFragment(args: Bundle?) =
    requestStartFragment(T::class.java, args)

/**
 * Performs [DownloaderScope.useService] with an [Intent] created using the type information of [SERVICE].
 * @see [DownloaderScope.useService]
 */
suspend inline fun <reified SERVICE : Service, R : Any?> DownloaderScope<*>.useService(
    noinline block: suspend (IBinder) -> R
) = useService(
    Intent().apply { setClassName(downloaderPackageName, SERVICE::class.qualifiedName!!) }, block
)