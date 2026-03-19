package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.util.Log
import app.revanced.library.mostCommonCompatibleVersions
import app.revanced.manager.R
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.data.room.sources.Source as SourceInfo
import app.revanced.manager.domain.sources.APIPatchBundle
import app.revanced.manager.domain.sources.JsonPatchBundle
import app.revanced.manager.domain.sources.LocalPatchBundle
import app.revanced.manager.domain.sources.PatchBundleSource
import app.revanced.manager.domain.manager.SourceManager
import app.revanced.manager.domain.sources.Loader
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.patcher.patch.PatchBundle
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.util.tag
import kotlinx.collections.immutable.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.map
import kotlin.text.ifEmpty

private typealias Info = PersistentMap<Int, PatchBundleInfo.Global>

class PatchBundleRepository(
    app: Application,
    db: AppDatabase,
) : SourceManager<PatchBundleEntity, PatchBundle, Info>(
    persistentMapOf(),
    app.getDir("patch_bundles", Context.MODE_PRIVATE)
) {
    private val dao = db.patchBundleDao()

    override val updateFailed = R.string.patches_download_fail
    override val updateSuccess = R.string.patches_update_success
    override val updateUnavailable = R.string.patches_update_unavailable
    override val replaceFail = R.string.patches_replace_fail

    override suspend fun dbGetAll() = dao.all()
    override suspend fun dbGetProps(uid: Int) = dao.getProps(uid)
    override suspend fun dbUpsert(entity: PatchBundleEntity) = dao.upsert(entity)
    override suspend fun dbRemove(uid: Int) = dao.remove(uid)
    override suspend fun dbReset() = dao.reset()

    override fun loadEntity(entity: PatchBundleEntity): PatchBundleSource = with(entity) {
        val file = directoryOf(uid).resolve("patches.jar")
        val actualName =
            entity.name.ifEmpty { app.getString(if (uid == 0) R.string.patches_name_default else R.string.source_name_fallback) }

        return when (source) {
            is SourceInfo.Local -> LocalPatchBundle(actualName, uid, null, file, PatchBundleLoader)
            is SourceInfo.API -> APIPatchBundle(
                actualName,
                uid,
                versionHash,
                null,
                file,
                SourceInfo.API.SENTINEL,
                autoUpdate,
                PatchBundleLoader
            ) { getPatchesUpdate() }

            is SourceInfo.Remote -> JsonPatchBundle(
                actualName,
                uid,
                versionHash,
                null,
                file,
                source.url.toString(),
                autoUpdate,
                PatchBundleLoader
            )
        }
    }

    override fun entityFromProps(
        uid: Int,
        props: SourceProperties
    ) = PatchBundleEntity(
        uid,
        name = props.name,
        versionHash = props.versionHash,
        source = props.source,
        autoUpdate = props.autoUpdate
    )

    override fun realNameOf(loaded: PatchBundle) = loaded.manifestAttributes?.name
    override suspend fun loadDataFromSources(sources: MutableMap<Int, Source<PatchBundle>>) = loadMetadata(sources).toPersistentMap()

    val sources = store.state.map { it.sources.values.toList() }
    val bundles = store.state.map {
        it.sources.mapNotNull { (uid, src) ->
            uid to (src.loaded ?: return@mapNotNull null)
        }.toMap()
    }
    val bundleInfoFlow = store.state.map { it.data }

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

    private suspend fun loadMetadata(sources: MutableMap<Int, PatchBundleSource>): Map<Int, PatchBundleInfo.Global> {
        // Map bundles -> sources
        val map = sources.mapNotNull { (_, src) ->
            (src.loaded ?: return@mapNotNull null) to src
        }.toMap()

        val metadata = try {
            runInterruptible(Dispatchers.Default) {
                PatchBundle.Loader.metadata(map.keys)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (error: Throwable) {
            sources.entries.forEach { entry ->
                entry.setValue(entry.value.copy(error = error))
            }

            Log.e(tag, "Failed to load bundles", error)
            emptyMap()
        }

        val output = buildMap {
            metadata.forEach { (bundle, result) ->
                val src = map[bundle]!!
                val error = result.exceptionOrNull()
                if (error != null) {
                    sources[src.uid] = src.copy(error = error)
                    return@forEach
                }

                this[src.uid] = PatchBundleInfo.Global(
                    src.name,
                    bundle.manifestAttributes?.version,
                    src.uid,
                    result.getOrThrow().toList()
                )
            }
        }

        return output
    }

    suspend fun isVersionAllowed(packageName: String, version: String) =
        withContext(Dispatchers.Default) {
            if (!prefs.suggestedVersionSafeguard.get()) return@withContext true

            val suggestedVersion = suggestedVersions.first()[packageName] ?: return@withContext true
            suggestedVersion == version
        }

    private companion object PatchBundleLoader : Loader<PatchBundle> {
        override fun load(file: File) = PatchBundle(file.absolutePath)
    }
}
