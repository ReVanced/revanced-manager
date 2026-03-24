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
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
abstract class SourceManager<DB : SourceManager.DatabaseEntity, LOADED, OUTPUT>(
    initial: OUTPUT,
    protected val sourceDir: File
) : KoinComponent {
    protected val app: Application by inject()
    protected val prefs: PreferencesManager by inject()
    protected val networkInfo: NetworkInfo by inject()

    protected abstract suspend fun dbGetAll(): List<DB>
    protected abstract suspend fun dbGetProps(uid: Int): SourceProperties?
    protected abstract suspend fun dbUpsert(entity: DB)
    protected abstract suspend fun dbRemove(uid: Int)
    protected abstract suspend fun dbReset()

    protected abstract fun loadEntity(entity: DB): Source<LOADED>
    protected abstract fun entityFromProps(uid: Int, props: SourceProperties): DB

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

    protected val defaultSource = entityFromProps(
        0, SourceProperties(
            name = "",
            versionHash = null,
            source = SourceInfo.API,
            autoUpdate = false
        )
    )

    protected val store = Store(
        CoroutineScope(Dispatchers.Default),
        State<LOADED, OUTPUT>(data = initial)
    )

    val updateErrors = store.state.map { it.updateErrors }
    val apiOutageError = updateErrors.map { it[0] }
    val hasOutdated = store.state.map { it.outdatedSources.isNotEmpty() }

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
    protected suspend fun doReload(oldState: State<LOADED, OUTPUT>): State<LOADED, OUTPUT> {
        val entities = loadFromDb().onEach {
            Log.d(tag, "Source: $it")
        }

        val sources = entities
            .associateTo(mutableMapOf()) { it.uid to loadEntity(it) }
        sources.forEach syncName@{ (uid, src) ->
            val newName = src.loaded?.let(::realNameOf).takeIf { it != src.name }
                ?: return@syncName

            updateDb(uid) {
                it.copy(
                    name = newName,
                    releasedAt = (src as? RemoteSource)?.releasedAt?.toEpochMillis()
                )
            }
            sources[uid] = src.copy(name = newName)
        }

        val data = loadDataFromSources(sources)
        return oldState.copy(data = data, sources = sources)
    }

    suspend fun reload() = dispatchAction("Full reload") {
        doReload(it)
    }

    private suspend fun loadFromDb(): List<DB> {
        val all = dbGetAll().toMutableList()
        val default = defaultSource

        if (all.none { it.uid == default.uid }) {
            dbUpsert(default)
            all += default
        }

        return all
    }

    private suspend fun createEntity(
        name: String,
        source: SourceInfo,
        autoUpdate: Boolean = false,
    ) =
        entityFromProps(
            uid = generateUid(),
            SourceProperties(
                name = name,
                versionHash = null,
                source = source,
                autoUpdate = autoUpdate,
                releasedAt = null,
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
                    releasedAt = new.releasedAt,
                )
            )
        )
    }

    protected fun directoryOf(uid: Int) = sourceDir.resolve(uid.toString()).also { it.mkdirs() }

    suspend fun reset() = dispatchAction("Reset") { state ->
        dbReset()
        state.sources.keys.forEach { directoryOf(it).deleteRecursively() }
        doReload(state.copy(updateErrors = emptyMap(), outdatedSources = emptySet()))
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
            State(
                data = data,
                sources = currentSources,
                updateErrors = state.updateErrors
                    .filter { (it, _) -> it in currentSources.keys },
                outdatedSources = state.outdatedSources.filterTo(mutableSetOf()) { it in currentSources.keys }
            )
        }

    suspend fun createLocal(createStream: suspend () -> InputStream) =
        dispatchAction("Add local") { state ->
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

            doReload(state)
        }

    suspend fun createRemote(url: String, autoUpdate: Boolean) =
        dispatchAction("Add remote ($url)") { state ->
            val entity = createEntity("", SourceInfo.from(url), autoUpdate)
            val src = loadEntity(entity) as RemoteSource<LOADED>
            update(src)
            state.copy(sources = state.sources.toMutableMap().also { it[src.uid] = src })
        }

    suspend fun reloadApiSources() = dispatchAction("Reload API sources") { state ->
        this@SourceManager.store.state.value.sources.values.filterIsInstance<APISource<LOADED>>()
            .forEach { src ->
                with(src) { deleteLocalFile() }
                updateDb(src.uid) { it.copy(versionHash = null, releasedAt = null) }
            }

        doReload(state)
    }

    suspend fun RemoteSource<LOADED>.setAutoUpdate(value: Boolean) =
        dispatchAction("Set auto update ($name, $value)") { state ->
            updateDb(uid) { it.copy(autoUpdate = value) }
            val newSrc = state.sources[uid]?.asRemoteOrNull?.copy(autoUpdate = value)
                ?: return@dispatchAction state

            state.copy(sources = state.sources.toMutableMap().also { it[uid] = newSrc })
        }

    suspend fun update(
        vararg sources: RemoteSource<LOADED>,
        showToast: Boolean = false,
        force: Boolean = true
    ) {
        val uids = sources.map { it.uid }.toSet()
        store.dispatch(Update(showToast = showToast, force = force) { it.uid in uids })
    }

    suspend fun redownloadRemote() =
        store.dispatch(Update(force = true, redownload = true))

    /**
     * Updates all sources that should be automatically updated.
     */
    suspend fun updateCheck(showToast: Boolean = false, force: Boolean = true) = store.dispatch(
        Update(
            showToast = showToast,
            force = force || prefs.allowMeteredNetworks.get()
        ) { it.autoUpdate }
    )

    private inner class Update(
        private val force: Boolean = false,
        private val redownload: Boolean = false,
        private val showToast: Boolean = false,
        private val predicate: (source: RemoteSource<LOADED>) -> Boolean = { true },
    ) : Action<State<LOADED, OUTPUT>> {
        private suspend fun toast(@StringRes id: Int, vararg args: Any?) =
            withContext(Dispatchers.Main) { app.toast(app.getString(id, *args)) }

        override fun toString() = if (redownload) "Redownload remote sources" else "Update check"

        override suspend fun ActionContext.execute(
            current: State<LOADED, OUTPUT>
        ) = supervisorScope {
            val checkOnly = !force && !networkInfo.isUnmetered()

            val errors = current.updateErrors.toMutableMap()
            val outdated = current.outdatedSources.toMutableSet()

            val results = current.sources.values
                .filterIsInstance<RemoteSource<LOADED>>()
                .filter { predicate(it) }
                .also { targets ->
                    // Clear errors for sources we are updating.
                    targets.forEach { src ->
                        errors.remove(src.uid)
                        outdated.remove(src.uid)
                    }
                }
                .map {
                    async update@{
                        Log.d(tag, "Updating: ${it.name}")

                        val updateResult = it.runCatching {
                            when {
                                redownload -> downloadLatest()
                                checkOnly -> getUpdateInfo()?.let { info -> RemoteSource.UpdateResult(info.version, info.createdAt) }
                                else -> update()
                            } ?: return@update null
                        }

                        it to updateResult
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
            if (results.isEmpty()) {
                if (showToast) toast(updateUnavailable)
                return@supervisorScope current.copy(
                    updateErrors = errors,
                    outdatedSources = outdated
                )
            }

            var hasErrors = false
            results.forEach { (src, result) ->
                result.getOrNull()?.let { updateResult ->
                    if (checkOnly) {
                        outdated.add(src.uid)
                        return@let
                    }

                    val name = src.loaded?.let(::realNameOf) ?: src.name
                    updateDb(src.uid) {
                        it.copy(
                            versionHash = updateResult.versionHash,
                            name = name,
                            releasedAt = updateResult.releasedAt.toEpochMillis()
                        )
                    }
                }
                result.exceptionOrNull()?.let {
                    errors[src.uid] = it
                    Log.e(tag, "Failed to update source (${src.uid})", it)
                    hasErrors = true
                }
            }

            when {
                !showToast -> {}
                hasErrors -> {
                    val error = errors.values.first()
                    toast(updateFailed, error)
                }

                else -> toast(updateSuccess)
            }
            doReload(
                current.copy(
                    updateErrors = errors,
                    outdatedSources = outdated
                )
            )
        }
    }

    data class State<LOADED, OUTPUT>(
        val data: OUTPUT,
        val sources: Map<Int, Source<LOADED>> = emptyMap(),
        val updateErrors: Map<Int, Throwable> = emptyMap(),
        val outdatedSources: Set<Int> = emptySet(),
    )

    interface DatabaseEntity {
        val uid: Int
    }
}

private fun LocalDateTime.toEpochMillis() = toInstant(TimeZone.UTC).toEpochMilliseconds()