package app.revanced.manager.domain.sources

import android.net.Uri
import androidx.compose.runtime.Stable
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.domain.protocol.ProtocolHandler
import app.revanced.manager.domain.protocol.getStream
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.utils.APIFailure
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

fun interface Loader<T> {
    fun load(file: File): T
}

typealias PatchBundleSource = Source<PatchBundle>

class UnsupportedSourceException(cause: Throwable? = null) : Exception(cause)

internal fun Throwable.asSourceException(): Throwable {
    if (this is UnsupportedSourceException) return this

    val hasSerializationFailure = generateSequence(this) { it.cause }
        .any { it is SerializationException }
    if (!hasSerializationFailure) return this

    return when (this) {
        is APIFailure -> UnsupportedSourceException(this)
        else -> UnsupportedSourceException(this)
    }
}

// A resource and the URL it is retrieved from.
@Stable
class Source<T>(
    val name: String,
    val uid: Int,
    val uri: Uri,
    val versionHash: String?,
    val releasedAt: LocalDateTime?,
    val autoUpdate: Boolean,
    error: Throwable?,
    private val file: File,
    private val loader: Loader<T>,
    private val handlers: Map<String, ProtocolHandler>,
    private val json: Json
) {
    val state = when {
        error != null -> State.Failed(error)
        !hasInstalled() -> State.Missing
        else -> try {
            State.Available(loader.load(file))
        } catch (t: Throwable) {
            State.Failed(t)
        }
    }

    val isDefault inline get() = uid == 0

    val loaded get() = @Suppress("UNCHECKED_CAST") (state as? State.Available<T>)?.obj
    val error get() = (state as? State.Failed)?.throwable

    data class UpdateResult(val versionHash: String, val releasedAt: LocalDateTime)

    suspend fun ActionContext.deleteFile() = withContext(Dispatchers.IO) {
        file.delete()
    }

    fun copy(
        error: Throwable? = this.error,
        name: String = this.name,
        uri: Uri = this.uri,
        autoUpdate: Boolean = this.autoUpdate,
        versionHash: String? = this.versionHash,
        releasedAt: LocalDateTime? = this.releasedAt
    ) = Source(
        name,
        uid,
        uri,
        versionHash,
        releasedAt,
        autoUpdate,
        error,
        file,
        loader,
        handlers,
        json
    )

    private fun hasInstalled() = file.exists()

    private fun outputStream(): OutputStream = with(file) {
        // Android 14+ requires dex containers to be readonly.
        try {
            setWritable(true, true)
            outputStream()
        } finally {
            setReadOnly()
        }
    }

    private suspend fun getLatestInfo(): ReVancedAsset = withContext(Dispatchers.IO) {
        runCatching {
            handlers.getStream(uri) { stream ->
                json.decodeFromString<ReVancedAsset>(stream.reader().readText())
            }
        }.getOrElse { throw it.asSourceException() }
    }

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

    // Replaces the content with the resource behind [uri], e.g. an imported file.
    suspend fun ActionContext.replace(uri: Uri) {
        withContext(Dispatchers.IO) {
            handlers.getStream(uri) { stream ->
                outputStream().use { stream.copyTo(it) }
            }
        }
    }

    sealed interface State {
        data object Missing : State
        data class Failed(val throwable: Throwable) : State
        data class Available<T>(val obj: T) : State
    }
}

object Extensions {
    val PatchBundleSource.version get() = loaded?.manifestAttributes?.version
}
