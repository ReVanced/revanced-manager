package app.revanced.manager.util.saver

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
class Nullable<T>(val inner: @RawValue T?) : Parcelable

/**
 * Creates a saver that can save nullable versions of types that have custom savers.
 */
fun <Original : Any, Saveable : Any> nullableSaver(baseSaver: Saver<Original, Saveable>): Saver<Original?, Nullable<Saveable>> =
    Saver(
        save = { value ->
            with(baseSaver) {
                save(value ?: return@Saver Nullable(null))
            }?.let(::Nullable)
        },
        restore = {
            it.inner?.let(baseSaver::restore)
        }
    )