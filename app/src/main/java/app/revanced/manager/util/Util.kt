package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Data
import androidx.work.workDataOf
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

typealias PatchesSelection = Map<String, List<String>>

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

fun Context.loadIcon(string: String): Drawable? {
    return try {
        packageManager.getApplicationIcon(string)
    } catch (e: NameNotFoundException) {
        null
    }
}

fun Context.toast(string: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, string, duration).show()
}

fun String.parseUrlOrNull() = try {
    Url(this)
} catch (_: Throwable) {
    null
}

/**
 * Safely perform an operation that may fail to avoid crashing the app.
 * If [block] fails, the error will be logged and a toast will be shown to the user to inform them that the action failed.
 *
 * @param context The android [Context].
 * @param toastMsg The toast message to show if [block] throws.
 * @param logMsg The log message.
 * @param block The code to execute.
 */
inline fun uiSafe(context: Context, @StringRes toastMsg: Int, logMsg: String, block: () -> Unit) {
    try {
        block()
    } catch (error: Exception) {
        context.toast(
            context.getString(
                toastMsg,
                error.message ?: error.cause?.message ?: error::class.simpleName
            )
        )
        Log.e(tag, logMsg, error)
    }
}

inline fun LifecycleOwner.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

const val workDataKey = "payload"

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> T.serialize(): Data =
    workDataOf(workDataKey to Cbor.Default.encodeToByteArray(this))

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Data.deserialize(): T? =
    getByteArray(workDataKey)?.let { Cbor.Default.decodeFromByteArray(it) }