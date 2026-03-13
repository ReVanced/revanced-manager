package app.revanced.manager.domain.sources

import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

typealias LocalPatchBundle = LocalSource<PatchBundle>

class LocalSource<T>(
    name: String,
    uid: Int,
    error: Throwable?,
    file: File,
    loader: Loader<T>
) : Source<T>(name, uid, error, file, loader) {
    suspend fun ActionContext.replace(patches: InputStream) {
        withContext(Dispatchers.IO) {
            outputStream().use { outputStream ->
                patches.copyTo(outputStream)
            }
        }
    }

    override fun copy(error: Throwable?, name: String) = LocalSource(
        name,
        uid,
        error,
        file,
        loader
    )
}
