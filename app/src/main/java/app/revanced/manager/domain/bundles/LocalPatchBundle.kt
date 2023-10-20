package app.revanced.manager.domain.bundles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LocalPatchBundle(name: String, id: Int, directory: File) : PatchBundleSource(name, id, directory) {
    suspend fun replace(patches: InputStream? = null, integrations: InputStream? = null) {
        withContext(Dispatchers.IO) {
            patches?.let { inputStream ->
                patchBundleOutputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            integrations?.let {
                Files.copy(it, this@LocalPatchBundle.integrationsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        reload()
    }
}
