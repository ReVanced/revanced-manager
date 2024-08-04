package app.revanced.manager.domain.manager.base

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.domain.manager.base.BasePreferencesManager.Companion.editor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

abstract class BasePreferencesManager(private val context: Context, name: String) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = name)
    protected val dataStore get() = context.dataStore

    suspend fun preload() {
        dataStore.data.first()
    }

    suspend fun edit(block: EditorContext.() -> Unit) = dataStore.editor(block)

    protected fun stringPreference(key: String, default: String) =
        StringPreference(dataStore, key, default)

    protected fun booleanPreference(key: String, default: Boolean) =
        BooleanPreference(dataStore, key, default)

    protected fun intPreference(key: String, default: Int) = IntPreference(dataStore, key, default)

    protected fun floatPreference(key: String, default: Float) =
        FloatPreference(dataStore, key, default)

    protected inline fun <reified E : Enum<E>> enumPreference(
        key: String,
        default: E
    ) = EnumPreference(dataStore, key, default, enumValues())

    companion object {
        suspend inline fun DataStore<Preferences>.editor(crossinline block: EditorContext.() -> Unit) {
            edit {
                EditorContext(it).run(block)
            }
        }
    }
}

class EditorContext(private val prefs: MutablePreferences) {
    var <T> Preference<T>.value
        get() = prefs.run { read() }
        set(value) = prefs.run { write(value) }
}

abstract class Preference<T>(
    private val dataStore: DataStore<Preferences>,
    val default: T
) {
    internal abstract fun Preferences.read(): T
    internal abstract fun MutablePreferences.write(value: T)

    val flow = dataStore.data.map { with(it) { read() } ?: default }.distinctUntilChanged()

    suspend fun get() = flow.first()
    fun getBlocking() = runBlocking { get() }
    @Composable
    fun getAsState() = flow.collectAsStateWithLifecycle(initialValue = remember {
        getBlocking()
    })
    suspend fun update(value: T) = dataStore.editor {
        this@Preference.value = value
    }
}

class EnumPreference<E : Enum<E>>(
    dataStore: DataStore<Preferences>,
    key: String,
    default: E,
    private val enumValues: Array<E>
) : Preference<E>(dataStore, default) {
    private val key = stringPreferencesKey(key)
    override fun Preferences.read() =
        this[key]?.let { name ->
            enumValues.find { it.name == name }
        } ?: default

    override fun MutablePreferences.write(value: E) {
        this[key] = value.name
    }
}

abstract class BasePreference<T>(dataStore: DataStore<Preferences>, default: T) :
    Preference<T>(dataStore, default) {
    protected abstract val key: Preferences.Key<T>
    override fun Preferences.read() = this[key] ?: default
    override fun MutablePreferences.write(value: T) {
        this[key] = value
    }
}

class StringPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: String
) : BasePreference<String>(dataStore, default) {
    override val key = stringPreferencesKey(key)
}

class BooleanPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: Boolean
) : BasePreference<Boolean>(dataStore, default) {
    override val key = booleanPreferencesKey(key)
}

class IntPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: Int
) : BasePreference<Int>(dataStore, default) {
    override val key = intPreferencesKey(key)
}

class FloatPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: Float
) : BasePreference<Float>(dataStore, default) {
    override val key = floatPreferencesKey(key)
}