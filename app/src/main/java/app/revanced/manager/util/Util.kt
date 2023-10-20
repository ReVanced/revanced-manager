package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Locale

typealias PatchesSelection = Map<Int, Set<String>>
typealias Options = Map<Int, Map<String, Map<String, Any?>>>

val Context.isDebuggable get() = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

fun Context.toast(string: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, string, duration).show()
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
@OptIn(DelicateCoroutinesApi::class)
inline fun uiSafe(context: Context, @StringRes toastMsg: Int, logMsg: String, block: () -> Unit) {
    try {
        block()
    } catch (error: Exception) {
        // You can only toast on the main thread.
        GlobalScope.launch(Dispatchers.Main) {
            context.toast(
                context.getString(
                    toastMsg,
                    error.simpleMessage()
                )
            )
        }

        Log.e(tag, logMsg, error)
    }
}

fun Throwable.simpleMessage() = this.message ?: this.cause?.message ?: this::class.simpleName

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

/**
 * Run [transformer] on the [Iterable] and then [combine] the result using [combiner].
 * This is used to transform collections that contain [Flow]s into something that is easier to work with.
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, reified R, C> Flow<Iterable<T>>.flatMapLatestAndCombine(
    crossinline combiner: (Array<R>) -> C,
    crossinline transformer: (T) -> Flow<R>,
): Flow<C> = flatMapLatest { iterable ->
    combine(iterable.map(transformer)) {
        combiner(it)
    }
}

val Color.hexCode: String
    inline get() {
        val a: Int = (alpha * 255).toInt()
        val r: Int = (red * 255).toInt()
        val g: Int = (green * 255).toInt()
        val b: Int = (blue * 255).toInt()
        return java.lang.String.format(Locale.getDefault(), "%02X%02X%02X%02X", r, g, b, a)
    }

suspend fun <T> Flow<Iterable<T>>.collectEach(block: suspend (T) -> Unit) {
    this.collect { iterable ->
        iterable.forEach {
            block(it)
        }
    }
}