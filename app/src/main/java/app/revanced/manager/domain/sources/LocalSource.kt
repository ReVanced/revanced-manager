package app.revanced.manager.domain.sources

import android.net.Uri
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.domain.protocol.ProtocolHandler
import app.revanced.manager.domain.protocol.getStream
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

typealias LocalPatchBundle = LocalSource<PatchBundle>

class LocalSource<T>(
    name: String,
    uid: Int,
    error: Throwable?,
    file: File,
    loader: Loader<T>,
    private val handlers: Map<String, ProtocolHandler>
) : Source<T>(name, uid, error, file, loader) {
    suspend fun ActionContext.replace(uri: Uri) {
        withContext(Dispatchers.IO) {
            handlers.getStream(uri) { stream ->
                outputStream().use { stream.copyTo(it) }
            }
        }
    }

    override fun copy(error: Throwable?, name: String) = LocalSource(
        name,
        uid,
        error,
        file,
        loader,
        handlers
    )
}
