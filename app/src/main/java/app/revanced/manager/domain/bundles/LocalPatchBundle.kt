package app.revanced.manager.domain.bundles

import app.revanced.manager.data.redux.ActionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class LocalPatchBundle(
    name: String,
    uid: Int,
    error: Throwable?,
    directory: File
) : PatchBundleSource(name, uid, error, directory) {
    suspend fun ActionContext.replace(patches: InputStream) {
        withContext(Dispatchers.IO) {
            patchBundleOutputStream().use { outputStream ->
                patches.copyTo(outputStream)
            }
        }
    }

    override fun copy(error: Throwable?, name: String) = LocalPatchBundle(
        name,
        uid,
        error,
        directory
    )
}
