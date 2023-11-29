package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.util.Log
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.domain.bundles.APIPatchBundle
import app.revanced.manager.domain.bundles.JsonPatchBundle
import app.revanced.manager.data.room.bundles.Source as SourceInfo
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.util.flatMapLatestAndCombine
import app.revanced.manager.util.tag
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class PatchBundleRepository(
    private val app: Application,
    private val persistenceRepo: PatchBundlePersistenceRepository,
    private val networkInfo: NetworkInfo,
) {
    private val bundlesDir = app.getDir("patch_bundles", Context.MODE_PRIVATE)

    private val _sources: MutableStateFlow<Map<Int, PatchBundleSource>> =
        MutableStateFlow(emptyMap())
    val sources = _sources.map { it.values.toList() }

    val bundles = sources.flatMapLatestAndCombine(
        combiner = {
            it.mapNotNull { (uid, state) ->
                val bundle = state.patchBundleOrNull() ?: return@mapNotNull null
                uid to bundle
            }.toMap()
        }
    ) {
        it.state.map { state -> it.uid to state }
    }

    /**
     * Get the directory of the [PatchBundleSource] with the specified [uid], creating it if needed.
     */
    private fun directoryOf(uid: Int) = bundlesDir.resolve(uid.toString()).also { it.mkdirs() }

    private fun PatchBundleEntity.load(): PatchBundleSource {
        val dir = directoryOf(uid)

        return when (source) {
            is SourceInfo.Local -> LocalPatchBundle(name, uid, dir)
            is SourceInfo.API -> APIPatchBundle(name, uid, dir, SourceInfo.API.SENTINEL)
            is SourceInfo.Remote -> JsonPatchBundle(
                name,
                uid,
                dir,
                source.url.toString()
            )
        }
    }

    suspend fun reload() = withContext(Dispatchers.Default) {
        val entities = persistenceRepo.loadConfiguration().onEach {
            Log.d(tag, "Bundle: $it")
        }

        _sources.value = entities.associate {
            it.uid to it.load()
        }
    }

    suspend fun reset() = withContext(Dispatchers.Default) {
        persistenceRepo.reset()
        _sources.value = emptyMap()
        bundlesDir.apply {
            deleteRecursively()
            mkdirs()
        }

        reload()
    }

    suspend fun remove(bundle: PatchBundleSource) = withContext(Dispatchers.Default) {
        persistenceRepo.delete(bundle.uid)
        directoryOf(bundle.uid).deleteRecursively()

        _sources.update {
            it.filterKeys { key ->
                key != bundle.uid
            }
        }
    }

    private fun addBundle(patchBundle: PatchBundleSource) =
        _sources.update { it.toMutableMap().apply { put(patchBundle.uid, patchBundle) } }

    suspend fun createLocal(name: String, patches: InputStream, integrations: InputStream?) {
        val id = persistenceRepo.create(name, SourceInfo.Local).uid
        val bundle = LocalPatchBundle(name, id, directoryOf(id))

        bundle.replace(patches, integrations)
        addBundle(bundle)
    }

    suspend fun createRemote(name: String, url: String, autoUpdate: Boolean) {
        val entity = persistenceRepo.create(name, SourceInfo.from(url), autoUpdate)
        addBundle(entity.load())
    }

    private suspend inline fun <reified T> getBundlesByType() =
        sources.first().filterIsInstance<T>()

    suspend fun reloadApiBundles() {
        getBundlesByType<APIPatchBundle>().forEach {
            it.deleteLocalFiles()
        }

        reload()
    }

    suspend fun redownloadRemoteBundles() =
        getBundlesByType<RemotePatchBundle>().forEach { it.downloadLatest() }

    suspend fun updateCheck() =
        uiSafe(app, R.string.source_download_fail, "Failed to update bundles") {
            coroutineScope {
                if (!networkInfo.isSafe()) {
                    Log.d(tag, "Skipping update check because the network is down or metered.")
                    return@coroutineScope
                }

                getBundlesByType<RemotePatchBundle>().forEach {
                    launch {
                        if (!it.propsFlow().first().autoUpdate) return@launch
                        Log.d(tag, "Updating patch bundle: ${it.name}")
                        it.update()
                    }
                }
            }
        }
}