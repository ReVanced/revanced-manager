package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.util.Log
import app.revanced.manager.data.room.sources.SourceEntity
import app.revanced.manager.data.room.sources.SourceLocation
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.util.flatMapLatestAndCombine
import app.revanced.manager.util.tag
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class SourceRepository(app: Application, private val persistenceRepo: SourcePersistenceRepository) {
    private val sourcesDir = app.getDir("sources", Context.MODE_PRIVATE)

    private val _sources: MutableStateFlow<Map<Int, Source>> = MutableStateFlow(emptyMap())
    val sources = _sources.map { it.values.toList() }

    val bundles = sources.flatMapLatestAndCombine(
        combiner = { it.toMap() }
    ) {
        it.bundle.map { bundle -> it.uid to bundle }
    }

    /**
     * Get the directory of the [Source] with the specified [uid], creating it if needed.
     */
    private fun directoryOf(uid: Int) = sourcesDir.resolve(uid.toString()).also { it.mkdirs() }

    private fun SourceEntity.load(dir: File) = when (location) {
        is SourceLocation.Local -> LocalSource(name, uid, dir)
        is SourceLocation.Remote -> RemoteSource(name, uid, dir)
    }

    suspend fun loadSources() = withContext(Dispatchers.Default) {
        val sourcesConfig = persistenceRepo.loadConfiguration().onEach {
            Log.d(tag, "Source: $it")
        }

        val sources = sourcesConfig.associate {
            val dir = directoryOf(it.uid)
            val source = it.load(dir)

            it.uid to source
        }

        _sources.emit(sources)
    }

    suspend fun resetConfig() = withContext(Dispatchers.Default) {
        persistenceRepo.clear()
        _sources.emit(emptyMap())
        sourcesDir.apply {
            deleteRecursively()
            mkdirs()
        }

        loadSources()
    }

    suspend fun remove(source: Source) = withContext(Dispatchers.Default) {
        persistenceRepo.delete(source.uid)
        directoryOf(source.uid).deleteRecursively()

        _sources.update {
            it.filterValues { value ->
                value.uid != source.uid
            }
        }
    }

    private fun addSource(source: Source) =
        _sources.update { it.toMutableMap().apply { put(source.uid, source) } }

    suspend fun createLocalSource(name: String, patches: InputStream, integrations: InputStream?) {
        val id = persistenceRepo.create(name, SourceLocation.Local)
        val source = LocalSource(name, id, directoryOf(id))

        addSource(source)

        source.replace(patches, integrations)
    }

    suspend fun createRemoteSource(name: String, apiUrl: Url) {
        val id = persistenceRepo.create(name, SourceLocation.Remote(apiUrl))
        addSource(RemoteSource(name, id, directoryOf(id)))
    }

    suspend fun redownloadRemoteSources() =
        sources.first().filterIsInstance<RemoteSource>().forEach { it.downloadLatest() }
}