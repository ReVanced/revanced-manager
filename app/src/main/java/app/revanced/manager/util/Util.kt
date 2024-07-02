package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.revanced.manager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

typealias PatchSelection = Map<Int, Set<String>>
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

fun Int.formatNumber(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        NumberFormatter.with()
            .notation(Notation.compactShort())
            .decimal(NumberFormatter.DecimalSeparatorDisplay.ALWAYS)
            .precision(Precision.fixedFraction(1))
            .locale(Locale.getDefault())
            .format(this)
            .toString()
    } else {
        val compact = CompactDecimalFormat.getInstance(
            Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT
        )
        compact.maximumFractionDigits = 1
        compact.format(this)
    }
}

fun String.relativeTime(context: Context): String {
    try {
        val currentTime = ZonedDateTime.now(ZoneId.of("UTC"))
        val inputDateTime = ZonedDateTime.parse(this)
        val duration = Duration.between(inputDateTime, currentTime)

        return when {
            duration.toMinutes() < 1 -> context.getString(R.string.just_now)
            duration.toMinutes() < 60 -> context.getString(R.string.minutes_ago, duration.toMinutes().toString())
            duration.toHours() < 24 -> context.getString(R.string.hours_ago, duration.toHours().toString())
            duration.toDays() < 30 -> context.getString(R.string.days_ago, duration.toDays().toString())
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                val formattedDate = inputDateTime.format(formatter)
                if (inputDateTime.year != currentTime.year) {
                    val yearFormatter = DateTimeFormatter.ofPattern(", yyyy")
                    val formattedYear = inputDateTime.format(yearFormatter)
                    "$formattedDate$formattedYear"
                } else {
                    formattedDate
                }
            }
        }
    } catch (e: DateTimeParseException) {
        return context.getString(R.string.invalid_date)
    }
}

@Composable
fun LazyListState.isScrollingUp(): State<Boolean> {
    return remember(this) {
        var previousIndex by mutableIntStateOf(firstVisibleItemIndex)
        var previousScrollOffset by mutableIntStateOf(firstVisibleItemScrollOffset)

        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
fun ScrollState.isScrollingUp(): State<Boolean> {
    return remember(this) {
        var previousScrollOffset by mutableIntStateOf(value)
        derivedStateOf {
            (previousScrollOffset >= value).also {
                previousScrollOffset = value
            }
        }
    }
}

val LazyListState.isScrollingUp: Boolean @Composable get() = this.isScrollingUp().value
val ScrollState.isScrollingUp: Boolean @Composable get() = this.isScrollingUp().value