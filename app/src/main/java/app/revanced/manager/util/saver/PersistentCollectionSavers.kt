package app.revanced.manager.util.saver

import androidx.compose.runtime.saveable.Saver
import kotlinx.collections.immutable.*

/**
 * Create a [Saver] for [PersistentList]s.
 */
fun <T> persistentListSaver() = Saver<PersistentList<T>, List<T>>(
    save = {
        it.toList()
    },
    restore = {
        it.toPersistentList()
    }
)

/**
 * Create a [Saver] for [PersistentSet]s.
 */
fun <T> persistentSetSaver() = Saver<PersistentSet<T>, Set<T>>(
    save = {
        it.toSet()
    },
    restore = {
        it.toPersistentSet()
    }
)

/**
 * Create a [Saver] for [PersistentMap]s.
 */
fun <K, V> persistentMapSaver() = Saver<PersistentMap<K, V>, Map<K, V>>(
    save = {
        it.toMap()
    },
    restore = {
        it.toPersistentMap()
    }
)

/**
 * Create a saver for [PersistentMap]s with a custom [Saver] used for the values.
 * Null values will not be saved by this [Saver].
 *
 * @param valueSaver The [Saver] used for the values of the [Map].
 */
fun <K, Original, Saveable : Any> persistentMapSaver(
    valueSaver: Saver<Original, Saveable>
) = Saver<PersistentMap<K, Original>, Map<K, Saveable>>(
    save = {
        buildMap {
            it.forEach { (key, value) ->
                with(valueSaver) {
                    save(value)?.let {
                        this@buildMap[key] = it
                    }
                }
            }
        }
    },
    restore = {
        buildMap {
            it.forEach { (key, value) ->
                this[key] = valueSaver.restore(value) ?: return@forEach
            }
        }.toPersistentMap()
    }
)