package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Clock

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
    } catch (e: CancellationException) {
        throw e
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
    crossinline combiner: suspend (Array<R>) -> C,
    crossinline transformer: suspend (T) -> Flow<R>,
): Flow<C> = flatMapLatest { iterable ->
    combine(iterable.map { transformer(it) }) {
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

fun LocalDateTime.relativeTime(context: Context): String {
    try {
        val now = Clock.System.now()
        val duration = now - this.toInstant(TimeZone.UTC)

        return when {
            duration.inWholeMinutes < 1 -> context.getString(R.string.just_now)
            duration.inWholeMinutes < 60 -> context.getString(
                R.string.minutes_ago,
                duration.inWholeMinutes.toString()
            )

            duration.inWholeHours < 24 -> context.getString(
                R.string.hours_ago,
                duration.inWholeHours.toString()
            )

            duration.inWholeHours < 30 -> context.getString(
                R.string.days_ago,
                duration.inWholeDays.toString()
            )

            else -> LocalDateTime.Format {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
                char(' ')
                day()
                if (now.toLocalDateTime(TimeZone.UTC).year != this@relativeTime.year) {
                    chars(", ")
                    year()
                }
            }.format(this)
        }
    } catch (e: IllegalArgumentException) {
        return context.getString(R.string.invalid_date)
    }
}

private var transparentListItemColorsCached: ListItemColors? = null

fun resetListItemColorsCached() {
    transparentListItemColorsCached = null
}

/**
 * The default ListItem colors, but with [ListItemColors.containerColor] set to [Color.Transparent].
 */
val transparentListItemColors
    @Composable get() = transparentListItemColorsCached
        ?: ListItemDefaults.colors(containerColor = Color.Transparent)
            .also { transparentListItemColorsCached = it }

@Composable
fun <T> EventEffect(
    flow: Flow<T>,
    vararg keys: Any?,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBlock by rememberUpdatedState(block)

    LaunchedEffect(flow, state, *keys) {
        lifecycleOwner.repeatOnLifecycle(state) {
            flow.collect {
                currentBlock(it)
            }
        }
    }
}

const val isScrollingUpSensitivity = 10

@Composable
fun LazyListState.isScrollingUp() = produceState(true, this) {
    var previousIndex = firstVisibleItemIndex
    var previousScrollOffset = firstVisibleItemScrollOffset

    snapshotFlow {
        firstVisibleItemIndex to firstVisibleItemScrollOffset
    }.collect { (index, scrollOffset) ->
        val indexChanged = previousIndex != index
        val offsetChanged = abs(previousScrollOffset - scrollOffset) > isScrollingUpSensitivity

        value = when {
            indexChanged -> previousIndex > index
            offsetChanged -> previousScrollOffset > scrollOffset
            else -> value
        }
        previousIndex = index
        previousScrollOffset = scrollOffset
    }
}

@Composable
fun ScrollState.isScrollingUp() = produceState(true, this) {
    var previousScrollOffset = this@isScrollingUp.value

    snapshotFlow { this@isScrollingUp.value }.collect { scrollOffset ->
        if (abs(previousScrollOffset - scrollOffset) > isScrollingUpSensitivity) {
            value = previousScrollOffset >= scrollOffset
        }

        previousScrollOffset = scrollOffset
    }
}

val LazyListState.isScrollingUp: Boolean @Composable get() = this.isScrollingUp().value
val ScrollState.isScrollingUp: Boolean @Composable get() = this.isScrollingUp().value

@Composable
@ReadOnlyComposable
fun <R> (() -> R).withHapticFeedback(constant: Int): () -> R {
    val view = LocalView.current
    return {
        view.performHapticFeedback(constant)
        this()
    }
}

@Composable
@ReadOnlyComposable
fun <T, R> ((T) -> R).withHapticFeedback(constant: Int): (T) -> R {
    val view = LocalView.current
    return {
        view.performHapticFeedback(constant)
        this(it)
    }
}

fun Modifier.enabled(condition: Boolean) = if (condition) this else alpha(0.5f)

@MainThread
fun <T : Any> SavedStateHandle.saveableVar(init: () -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> =
    PropertyDelegateProvider { _: Any?, property ->
        val name = property.name
        if (name !in this) this[name] = init()
        object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(name)!!
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                set(name, value)
        }
    }

fun <T : Any> SavedStateHandle.saveableVar(): ReadWriteProperty<Any?, T?> =
    object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? = get(property.name)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) =
            set(property.name, value)
    }

inline fun <reified T : Any> NavGraphBuilder.deepLinkedComposable(
    path: String,
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    val uri = "${BuildConfig.DEEP_LINK_SCHEME}://${path.trim('/')}"
    composable<T>(
        deepLinks = setOf(uri, "$uri/").map { navDeepLink<T>(basePath = it) }
    ) {
        content(it)
    }
}

fun androidx.navigation.NavController.navigateSafe(route: Any) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
    }
}

fun androidx.navigation.NavController.navigateSafe(
    route: Any,
    builder: androidx.navigation.NavOptionsBuilder.() -> Unit
) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route, builder)
    }
}

fun androidx.navigation.NavController.popBackStackSafe(): Boolean {
    return if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    } else {
        false
    }
}