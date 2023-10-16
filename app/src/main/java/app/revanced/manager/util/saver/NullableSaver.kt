package app.revanced.manager.util.saver

import androidx.compose.runtime.saveable.Saver
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Creates a saver that can save nullable versions of types that have custom savers.
 */
fun <Original : Any, Saveable : Any> nullableSaver(baseSaver: Saver<Original, Saveable>): Saver<Original?, Optional<Saveable>> =
    Saver(
        save = { value ->
            with(baseSaver) {
                save(value ?: return@Saver Optional.empty())
            }?.let {
                Optional.of(it)
            }
        },
        restore = {
            it.getOrNull()?.let(baseSaver::restore)
        }
    )