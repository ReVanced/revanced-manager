package app.revanced.manager.domain.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LocalSource(name: String, id: Int, directory: File) : Source(name, id, directory) {
    suspend fun replace(patches: InputStream? = null, integrations: InputStream? = null) {
        withContext(Dispatchers.IO) {
            patches?.let {
                Files.copy(it, patchesJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            integrations?.let {
                Files.copy(it, this@LocalSource.integrations.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        _bundle.emit(loadBundle { throw it })
    }
}
