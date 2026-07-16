package app.revanced.manager.domain.sources

import android.net.Uri
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.domain.protocol.ProtocolHandler
import app.revanced.manager.domain.protocol.getStream
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.utils.APIFailure
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlinx.serialization.SerializationException

typealias RemotePatchBundle = RemoteSource<PatchBundle>
typealias JsonPatchBundle = JsonSource<PatchBundle>
typealias APIPatchBundle = APISource<PatchBundle>

class UnsupportedRemoteSourceException(cause: Throwable? = null) : Exception(cause)

internal fun Throwable.asRemoteSourceException(): Throwable {
    if (this is UnsupportedRemoteSourceException) return this

    val hasSerializationFailure = generateSequence(this) { it.cause }
        .any { it is SerializationException }
    if (!hasSerializationFailure) return this

    return when (this) {
        is APIFailure -> UnsupportedRemoteSourceException(this)
        else -> UnsupportedRemoteSourceException(this)
    }
}

sealed class RemoteSource<T>(
    name: String,
    uid: Int,
    protected val versionHash: String?,
    val releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    val endpoint: String,
    val autoUpdate: Boolean,
    loader: Loader<T>,
    protected val handlers: Map<String, ProtocolHandler>
) : Source<T>(name, uid, error, file, loader) {
    data class UpdateResult(val versionHash: String, val releasedAt: LocalDateTime)

    protected abstract suspend fun getLatestInfo(): ReVancedAsset
    abstract fun copy(
        error: Throwable? = this.error,
        name: String = this.name,
        endpoint: String = this.endpoint,
        autoUpdate: Boolean = this.autoUpdate,
        versionHash: String? = this.versionHash,
        releasedAt: LocalDateTime? = this.releasedAt
    ): RemoteSource<T>

    override fun copy(error: Throwable?, name: String): RemoteSource<T> =
        copy(error, name, this.endpoint, this.autoUpdate, this.versionHash, this.releasedAt)

    private suspend fun download(info: ReVancedAsset) = withContext(Dispatchers.IO) {
        handlers.getStream(Uri.parse(info.downloadUrl)) { stream ->
            outputStream().use { stream.copyTo(it) }
        }

        UpdateResult(info.version, info.createdAt)
    }

    /**
     * Downloads the latest version regardless if there is a new update available.
     */
    suspend fun ActionContext.downloadLatest() = download(getLatestInfo())
    suspend fun ActionContext.getUpdateInfo() =
        getLatestInfo().takeUnless { hasInstalled() && it.version == versionHash }

    suspend fun ActionContext.update(): UpdateResult? = withContext(Dispatchers.IO) {
        getUpdateInfo()?.let { download(it) }
    }

    companion object {
        const val updateFailMsg = "Failed to update"
    }
}

class JsonSource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>,
    handlers: Map<String, ProtocolHandler>,
    private val json: Json
) : RemoteSource<T>(
    name, uid, versionHash, releasedAt, error, file, endpoint, autoUpdate, loader, handlers
) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        runCatching {
            handlers.getStream(Uri.parse(endpoint)) { stream ->
                json.decodeFromString<ReVancedAsset>(stream.reader().readText())
            }
        }.getOrElse { throw it.asRemoteSourceException() }
    }

    override fun copy(
        error: Throwable?,
        name: String,
        endpoint: String,
        autoUpdate: Boolean,
        versionHash: String?,
        releasedAt: LocalDateTime?
    ) = JsonSource(
        name,
        uid,
        versionHash,
        releasedAt,
        error,
        file,
        endpoint,
        autoUpdate,
        loader,
        handlers,
        json
    )
}

class APISource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>,
    handlers: Map<String, ProtocolHandler>,
    private val getUpdate: suspend ReVancedAPI.() -> APIResponse<ReVancedAsset>
) : RemoteSource<T>(
    name, uid, versionHash, releasedAt, error, file, endpoint, autoUpdate, loader, handlers
), KoinComponent {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = api.getUpdate().getOrThrow()
    override fun copy(
        error: Throwable?,
        name: String,
        endpoint: String,
        autoUpdate: Boolean,
        versionHash: String?,
        releasedAt: LocalDateTime?
    ) = APISource(
        name,
        uid,
        versionHash,
        releasedAt,
        error,
        file,
        endpoint,
        autoUpdate,
        loader,
        handlers,
        getUpdate
    )
}
