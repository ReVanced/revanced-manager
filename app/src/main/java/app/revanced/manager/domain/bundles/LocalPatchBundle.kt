package app.revanced.manager.domain.bundles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class LocalPatchBundle(name: String, id: Int, directory: File) :
    PatchBundleSource(name, id, directory) {
    suspend fun replace(patches: InputStream) {
        withContext(Dispatchers.IO) {
            patchBundleOutputStream().use { outputStream ->
                patches.copyTo(outputStream)
            }
        }

        reload()?.also {
            saveVersionHash(it.patchBundleManifestAttributes?.version)
        }
    }
}
