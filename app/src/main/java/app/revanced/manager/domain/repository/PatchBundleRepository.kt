package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import app.revanced.library.mostCommonCompatibleVersions
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.redux.Action
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.data.redux.Store
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.data.room.bundles.PatchBundleProperties
import app.revanced.manager.data.room.bundles.Source
import app.revanced.manager.domain.bundles.APIPatchBundle
import app.revanced.manager.domain.bundles.JsonPatchBundle
import app.revanced.manager.data.room.bundles.Source as SourceInfo
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.patcher.patch.PatchBundle
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.collections.immutable.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.text.ifEmpty

class PatchBundleRepository(
    private val app: Application,
    private val networkInfo: NetworkInfo,
    private val prefs: PreferencesManager,
    db: AppDatabase,
) {
    private val dao = db.patchBundleDao()
    private val bundlesDir = app.getDir("patch_bundles", Context.MODE_PRIVATE)

    private val store = Store(CoroutineScope(Dispatchers.Default), State())

    val sources = store.state.map { it.sources.values.toList() }
    val bundles = store.state.map {
        it.sources.mapNotNull { (uid, src) ->
            uid to (src.patchBundle ?: return@mapNotNull null)
        }.toMap()
    }
    val bundleInfoFlow = store.state.map { it.info }

    fun scopedBundleInfoFlow(packageName: String, version: String?) = bundleInfoFlow.map {
        it.map { (_, bundleInfo) ->
            bundleInfo.forPackage(
                packageName,
                version
            )
        }
    }

    val patchCountsFlow = bundleInfoFlow.map { it.mapValues { (_, info) -> info.patches.size } }

    val suggestedVersions = bundleInfoFlow.map {
        val allPatches =
            it.values.flatMap { bundle -> bundle.patches.map(PatchInfo::toPatcherPatch) }.toSet()

        allPatches.mostCommonCompatibleVersions(countUnusedPatches = true)
            .mapValues { (_, versions) ->
                if (versions.keys.size < 2)
                    return@mapValues versions.keys.firstOrNull()

                // The entries are ordered from most compatible to least compatible.
                // If there are entries with the same number of compatible patches, older versions will be first, which is undesirable.
                // This means we have to pick the last entry we find that has the highest patch count.
                // The order may change in future versions of ReVanced Library.
                var currentHighestPatchCount = -1
                versions.entries.last { (_, patchCount) ->
                    if (patchCount >= currentHighestPatchCount) {
                        currentHighestPatchCount = patchCount
                        true
                    } else false
                }.key
            }
    }

    private suspend inline fun dispatchAction(
        name: String,
        crossinline block: suspend ActionContext.(current: State) -> State
    ) {
        store.dispatch(object : Action<State> {
            override suspend fun ActionContext.execute(current: State) = block(current)
            override fun toString() = name
        })
    }

    /**
     * Performs a reload. Do not call this outside of a store action.
     */
    private suspend fun doReload(): State {
        val entities = loadFromDb().onEach {
            Log.d(tag, "Bundle: $it")
        }

        val sources = entities.associate { it.uid to it.load() }.toPersistentMap()

        val hasOutOfDateNames = sources.values.any { it.isNameOutOfDate }
        if (hasOutOfDateNames) dispatchAction(
            "Sync names"
        ) { state ->
            val nameChanges = state.sources.mapNotNull { (_, src) ->
                if (!src.isNameOutOfDate) return@mapNotNull null
                val newName = src.patchBundle?.manifestAttributes?.name?.takeIf { it != src.name }
                    ?: return@mapNotNull null

                src.uid to newName
            }
            val sources = state.sources.toMutableMap()
            val info = state.info.toMutableMap()
            nameChanges.forEach { (uid, name) ->
                updateDb(uid) { it.copy(name = name) }
                sources[uid] = sources[uid]!!.copy(name = name)
                info[uid] = info[uid]?.copy(name = name) ?: return@forEach
            }

            State(sources.toPersistentMap(), info.toPersistentMap())
        }
        val info = loadMetadata(sources).toPersistentMap()

        return State(sources, info)
    }

    suspend fun reload() = dispatchAction("Full reload") {
        doReload()
    }

    private suspend fun loadFromDb(): List<PatchBundleEntity> {
        val all = dao.all()
        if (all.isEmpty()) {
            dao.upsert(defaultSource)
            return listOf(defaultSource)
        }

        return all
    }

    private suspend fun loadMetadata(sources: Map<Int, PatchBundleSource>): Map<Int, PatchBundleInfo.Global> {
        // Map bundles -> sources
        val map = sources.mapNotNull { (_, src) ->
            (src.patchBundle ?: return@mapNotNull null) to src
        }.toMap()

        val metadata = try {
            PatchBundle.Loader.metadata(map.keys)
        } catch (error: Throwable) {
            val uids = map.values.map { it.uid }

            dispatchAction("Mark bundles as failed") { state ->
                state.copy(sources = state.sources.mutate {
                    uids.forEach { uid ->
                        it[uid] = it[uid]?.copy(error = error) ?: return@forEach
                    }
                })
            }

            Log.e(tag, "Failed to load bundles", error)
            emptyMap()
        }

        return metadata.entries.associate { (bundle, patches) ->
            val src = map[bundle]!!
            src.uid to PatchBundleInfo.Global(
                src.name,
                bundle.manifestAttributes?.version,
                src.uid,
                patches
            )
        }
    }

    suspend fun isVersionAllowed(packageName: String, version: String) =
        withContext(Dispatchers.Default) {
            if (!prefs.suggestedVersionSafeguard.get()) return@withContext true

            val suggestedVersion = suggestedVersions.first()[packageName] ?: return@withContext true
            suggestedVersion == version
        }

    /**
     * Get the directory of the [PatchBundleSource] with the specified [uid], creating it if needed.
     */
    private fun directoryOf(uid: Int) = bundlesDir.resolve(uid.toString()).also { it.mkdirs() }

    private fun PatchBundleEntity.load(): PatchBundleSource {
        val dir = directoryOf(uid)
        val actualName =
            name.ifEmpty { app.getString(if (uid == 0) R.string.patches_name_default else R.string.patches_name_fallback) }

        return when (source) {
            is SourceInfo.Local -> LocalPatchBundle(actualName, uid, null, dir)
            is SourceInfo.API -> APIPatchBundle(
                actualName,
                uid,
                versionHash,
                null,
                dir,
                SourceInfo.API.SENTINEL,
                autoUpdate,
            )

            is SourceInfo.Remote -> JsonPatchBundle(
                actualName,
                uid,
                versionHash,
                null,
                dir,
                source.url.toString(),
                autoUpdate,
            )
        }
    }

    private suspend fun createEntity(name: String, source: Source, autoUpdate: Boolean = false) =
        PatchBundleEntity(
            uid = generateUid(),
            name = name,
            versionHash = null,
            source = source,
            autoUpdate = autoUpdate
        ).also {
            dao.upsert(it)
        }

    /**
     * Updates a patch bundle in the database. Do not use this outside an action.
     */
    private suspend fun updateDb(
        uid: Int,
        block: (PatchBundleProperties) -> PatchBundleProperties
    ) {
        val previous = dao.getProps(uid)!!
        val new = block(previous)
        dao.upsert(
            PatchBundleEntity(
                uid = uid,
                name = new.name,
                versionHash = new.versionHash,
                source = new.source,
                autoUpdate = new.autoUpdate,
            )
        )
    }

    suspend fun reset() = dispatchAction("Reset") { state ->
        dao.reset()
        state.sources.keys.forEach { directoryOf(it).deleteRecursively() }
        doReload()
    }

    suspend fun remove(vararg bundles: PatchBundleSource) =
        dispatchAction("Remove (${bundles.map { it.uid }.joinToString(",")})") { state ->
            val sources = state.sources.toMutableMap()
            val info = state.info.toMutableMap()
            bundles.forEach {
                if (it.isDefault) return@forEach

                dao.remove(it.uid)
                directoryOf(it.uid).deleteRecursively()
                sources.remove(it.uid)
                info.remove(it.uid)
            }

            State(sources.toPersistentMap(), info.toPersistentMap())
        }

    suspend fun createLocal(createStream: suspend () -> InputStream) = dispatchAction("Add bundle") {
        with(createEntity("", SourceInfo.Local).load() as LocalPatchBundle) {
            try {
                createStream().use { patches -> replace(patches) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(tag, "Got exception while importing bundle", e)
                withContext(Dispatchers.Main) {
                    app.toast(app.getString(R.string.patches_replace_fail, e.simpleMessage()))
                }

                deleteLocalFile()
            }
        }

        doReload()
    }

    suspend fun createRemote(url: String, autoUpdate: Boolean) =
        dispatchAction("Add bundle ($url)") { state ->
            val src = createEntity("", SourceInfo.from(url), autoUpdate).load() as RemotePatchBundle
            update(src)
            state.copy(sources = state.sources.put(src.uid, src))
        }

    suspend fun reloadApiBundles() = dispatchAction("Reload API bundles") {
        this@PatchBundleRepository.sources.first().filterIsInstance<APIPatchBundle>().forEach {
            with(it) { deleteLocalFile() }
            updateDb(it.uid) { it.copy(versionHash = null) }
        }

        doReload()
    }

    suspend fun RemotePatchBundle.setAutoUpdate(value: Boolean) =
        dispatchAction("Set auto update ($name, $value)") { state ->
            updateDb(uid) { it.copy(autoUpdate = value) }
            val newSrc = (state.sources[uid] as? RemotePatchBundle)?.copy(autoUpdate = value)
                ?: return@dispatchAction state

            state.copy(sources = state.sources.put(uid, newSrc))
        }

    suspend fun update(vararg sources: RemotePatchBundle, showToast: Boolean = false) {
        val uids = sources.map { it.uid }.toSet()
        store.dispatch(Update(showToast = showToast) { it.uid in uids })
    }

    suspend fun redownloadRemoteBundles() = store.dispatch(Update(force = true))

    /**
     * Updates all bundles that should be automatically updated.
     */
    suspend fun updateCheck() = store.dispatch(Update { it.autoUpdate })

    private inner class Update(
        private val force: Boolean = false,
        private val showToast: Boolean = false,
        private val predicate: (bundle: RemotePatchBundle) -> Boolean = { true },
    ) : Action<State> {
        private suspend fun toast(@StringRes id: Int, vararg args: Any?) =
            withContext(Dispatchers.Main) { app.toast(app.getString(id, *args)) }

        override fun toString() = if (force) "Redownload remote bundles" else "Update check"

        override suspend fun ActionContext.execute(
            current: State
        ) = coroutineScope {
            if (!networkInfo.isSafe()) {
                Log.d(tag, "Skipping update check because the network is down or metered.")
                return@coroutineScope current
            }

            val updated = current.sources.values
                .filterIsInstance<RemotePatchBundle>()
                .filter { predicate(it) }
                .map {
                    async {
                        Log.d(tag, "Updating patch bundle: ${it.name}")

                        val newVersion = with(it) {
                            if (force) downloadLatest() else update()
                        } ?: return@async null

                        it to newVersion
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
            if (updated.isEmpty()) {
                if (showToast) toast(R.string.patches_update_unavailable)
                return@coroutineScope current
            }

            updated.forEach { (src, newVersionHash) ->
                val name = src.patchBundle?.manifestAttributes?.name ?: src.name

                updateDb(src.uid) {
                    it.copy(versionHash = newVersionHash, name = name)
                }
            }

            if (showToast) toast(R.string.patches_update_success)
            doReload()
        }

        override suspend fun catch(exception: Exception) {
            Log.e(tag, "Failed to update patches", exception)
            toast(R.string.patches_download_fail, exception.simpleMessage())
        }
    }

    data class State(
        val sources: PersistentMap<Int, PatchBundleSource> = persistentMapOf(),
        val info: PersistentMap<Int, PatchBundleInfo.Global> = persistentMapOf()
    )

    private companion object {
        val defaultSource = PatchBundleEntity(
            uid = 0,
            name = "",
            versionHash = null,
            source = Source.API,
            autoUpdate = false
        )
    }
}