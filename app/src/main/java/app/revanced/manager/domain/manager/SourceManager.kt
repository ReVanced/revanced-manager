package app.revanced.manager.domain.manager

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.redux.Action
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.data.redux.Store
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.sources.Source as SourceInfo
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.domain.sources.APISource
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.InputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Abstraction for managing a source system. Used by [app.revanced.manager.domain.repository.PatchBundleRepository] and [app.revanced.manager.domain.repository.DownloaderRepository].
 */
abstract class SourceManager<DB, LOADED, OUTPUT>(
    initial: OUTPUT,
    protected val sourceDir: File
) : KoinComponent {
    protected val app: Application by inject()
    protected val prefs: PreferencesManager by inject()
    protected val networkInfo: NetworkInfo by inject()

    protected abstract val defaultSource: DB

    protected abstract suspend fun dbGetAll(): List<DB>
    protected abstract suspend fun dbGetProps(uid: Int): SourceProperties?
    protected abstract suspend fun dbUpsert(entity: DB)
    protected abstract suspend fun dbRemove(uid: Int)
    protected abstract suspend fun dbReset()

    protected abstract fun loadEntity(entity: DB): Source<LOADED>
    protected abstract fun entityFromProps(uid: Int, props: SourceProperties): DB

    protected abstract fun uidOf(entity: DB): Int
    protected abstract fun realNameOf(loaded: LOADED): String?

    @get:StringRes
    protected abstract val updateUnavailable: Int
    @get:StringRes
    protected abstract val updateSuccess: Int
    @get:StringRes
    protected abstract val updateFailed: Int
    @get:StringRes
    protected abstract val replaceFail: Int

    protected abstract suspend fun loadDataFromSources(sources: MutableMap<Int, Source<LOADED>>): OUTPUT

    private val _updateError = MutableStateFlow<Throwable?>(null)
    val updateError = _updateError.asStateFlow()

    private val _apiOutageError = MutableStateFlow<Throwable?>(null)
    val apiOutageError = _apiOutageError.asStateFlow()

    protected val store =
        Store(CoroutineScope(Dispatchers.Default), State<LOADED, OUTPUT>(data = initial))

    protected suspend inline fun dispatchAction(
        name: String,
        crossinline block: suspend ActionContext.(current: State<LOADED, OUTPUT>) -> State<LOADED, OUTPUT>
    ) {
        store.dispatch(object : Action<State<LOADED, OUTPUT>> {
            override suspend fun ActionContext.execute(current: State<LOADED, OUTPUT>) =
                block(current)

            override fun toString() = name
        })
    }

    /**
     * Performs a reload. Do not call this outside of a store action.
     */
    protected suspend fun doReload(): State<LOADED, OUTPUT> {
        val entities = loadFromDb().onEach {
            Log.d(tag, "Source: $it")
        }

        val sources = entities
            .associateTo(mutableMapOf()) { uidOf(it) to loadEntity(it) }
        sources.forEach syncName@{ (uid, src) ->
            val newName = src.loaded?.let(::realNameOf).takeIf { it != src.name }
                ?: return@syncName

            updateDb(uid) { it.copy(name = newName) }
            sources[uid] = src.copy(name = newName)
        }

        val data = loadDataFromSources(sources)
        return State(sources.toPersistentMap(), data)
    }

    suspend fun reload() = dispatchAction("Full reload") {
        doReload()
    }

    private suspend fun loadFromDb(): List<DB> {
        val all = dbGetAll()
        if (all.isEmpty()) {
            dbUpsert(defaultSource)
            return listOf(defaultSource)
        }

        return all
    }

    private suspend fun createEntity(
        name: String,
        source: SourceInfo,
        autoUpdate: Boolean = false
    ) =
        entityFromProps(
            uid = generateUid(),
            SourceProperties(
                name = name,
                versionHash = null,
                source = source,
                autoUpdate = autoUpdate,
            )
        ).also {
            dbUpsert(it)
        }

    /**
     * Updates a source in the database. Do not use this outside an action.
     */
    private suspend fun updateDb(
        uid: Int,
        block: (SourceProperties) -> SourceProperties
    ) {
        val previous = dbGetProps(uid)!!
        val new = block(previous)
        dbUpsert(
            entityFromProps(
                uid = uid,
                SourceProperties(
                    name = new.name,
                    versionHash = new.versionHash,
                    source = new.source,
                    autoUpdate = new.autoUpdate,
                )
            )
        )
    }

    protected fun directoryOf(uid: Int) = sourceDir.resolve(uid.toString()).also { it.mkdirs() }

    suspend fun reset() = dispatchAction("Reset") { state ->
        dbReset()
        state.sources.keys.forEach { directoryOf(it).deleteRecursively() }
        doReload()
    }

    suspend fun remove(vararg sources: Source<LOADED>) =
        dispatchAction("Remove (${sources.map { it.uid }.joinToString(",")})") { state ->
            val currentSources = state.sources.toMutableMap()
            sources.forEach {
                if (it.isDefault) return@forEach

                dbRemove(it.uid)
                directoryOf(it.uid).deleteRecursively()
                currentSources.remove(it.uid)
            }

            val data = loadDataFromSources(currentSources)
            State(currentSources.toPersistentMap(), data)
        }

    suspend fun createLocal(createStream: suspend () -> InputStream) =
        dispatchAction("Add local") {
            val entity = createEntity("", SourceInfo.Local)
            with(loadEntity(entity) as LocalSource<LOADED>) {
                try {
                    createStream().use { patches -> replace(patches) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(tag, "Got exception while creating local source", e)
                    withContext(Dispatchers.Main) {
                        app.toast(app.getString(replaceFail, e.simpleMessage()))
                    }

                    deleteLocalFile()
                }
            }

            doReload()
        }

    suspend fun createRemote(url: String, autoUpdate: Boolean) =
        dispatchAction("Add remote ($url)") { state ->
            val entity = createEntity("", SourceInfo.from(url), autoUpdate)
            val src = loadEntity(entity) as RemoteSource<LOADED>
            update(src, force = true)
            state.copy(sources = state.sources.put(src.uid, src))
        }

    suspend fun reloadApiSources() = dispatchAction("Reload API sources") {
        this@SourceManager.store.state.value.sources.values.filterIsInstance<APISource<LOADED>>()
            .forEach { src ->
                with(src) { deleteLocalFile() }
                updateDb(src.uid) { it.copy(versionHash = null) }
            }

        doReload()
    }

    suspend fun RemoteSource<LOADED>.setAutoUpdate(value: Boolean) =
        dispatchAction("Set auto update ($name, $value)") { state ->
            updateDb(uid) { it.copy(autoUpdate = value) }
            val newSrc = (state.sources[uid] as? RemoteSource<LOADED>)?.copy(autoUpdate = value)
                ?: return@dispatchAction state

            state.copy(sources = state.sources.put(uid, newSrc))
        }

    suspend fun update(
        vararg sources: RemoteSource<LOADED>,
        showToast: Boolean = false,
        force: Boolean = false
    ) {
        val uids = sources.map { it.uid }.toSet()
        store.dispatch(Update(showToast = showToast, force = force) { it.uid in uids })
    }

    suspend fun redownloadRemote() =
        store.dispatch(Update(force = true, redownload = true))

    /**
     * Updates all sources that should be automatically updated.
     */
    suspend fun updateCheck() =
        store.dispatch(Update(force = prefs.allowMeteredNetworks.get()) { it.autoUpdate })

    private inner class Update(
        private val force: Boolean = false,
        private val redownload: Boolean = false,
        private val showToast: Boolean = false,
        private val predicate: (source: RemoteSource<LOADED>) -> Boolean = { true },
    ) : Action<State<LOADED, OUTPUT>> {
        private var attemptedMainApiUpdate = false

        private suspend fun toast(@StringRes id: Int, vararg args: Any?) =
            withContext(Dispatchers.Main) { app.toast(app.getString(id, *args)) }

        override fun toString() = if (redownload) "Redownload remote sources" else "Update check"

        override suspend fun ActionContext.execute(
            current: State<LOADED, OUTPUT>
        ) = coroutineScope {
            if (!networkInfo.isSafe(force)) {
                Log.d(tag, "Skipping update check because the network is down or metered.")
                return@coroutineScope current
            }

            val updated = current.sources.values
                .filterIsInstance<RemoteSource<LOADED>>()
                .filter { predicate(it) }
                .also { targets ->
                    attemptedMainApiUpdate = targets.any { it.uid == 0 && it is APISource<*> }
                }
                .map {
                    async {
                        Log.d(tag, "Updating: ${it.name}")

                        val newVersion = with(it) {
                            if (redownload) downloadLatest() else update()
                        } ?: return@async null

                        it to newVersion
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
            if (updated.isEmpty()) {
                if (showToast) toast(updateUnavailable)
                return@coroutineScope current
            }

            updated.forEach { (src, newVersionHash) ->
                val name = src.loaded?.let(::realNameOf) ?: src.name

                updateDb(src.uid) {
                    it.copy(versionHash = newVersionHash, name = name)
                }
            }

            if (showToast) toast(updateSuccess)
            _updateError.value = null
            if (attemptedMainApiUpdate) _apiOutageError.value = null
            doReload()
        }

        override suspend fun catch(exception: Exception) {
            Log.e(tag, "Failed to update", exception)
            _updateError.value = exception
            if (attemptedMainApiUpdate) _apiOutageError.value = exception
            toast(updateFailed, exception.simpleMessage())
        }
    }

    data class State<LOADED, OUTPUT>(
        val sources: PersistentMap<Int, Source<LOADED>> = persistentMapOf(),
        val data: OUTPUT
    )
}