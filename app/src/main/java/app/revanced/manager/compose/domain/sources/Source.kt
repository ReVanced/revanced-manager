package app.revanced.manager.compose.domain.sources

import android.util.Log
import androidx.compose.runtime.Stable
import app.revanced.manager.compose.patcher.patch.PatchBundle
import app.revanced.manager.compose.domain.repository.SourcePersistenceRepository
import app.revanced.manager.compose.util.tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * A [PatchBundle] source.
 */
@Stable
sealed class Source(val id: Int, directory: File) : KoinComponent {
    private val configRepository: SourcePersistenceRepository by inject()
    protected companion object {
        /**
         * A placeholder [PatchBundle].
         */
        val emptyPatchBundle = PatchBundle(emptyList(), null)
        fun logError(err: Throwable) {
            Log.e(tag, "Failed to load bundle", err)
        }
    }

    protected val patchesJar = directory.resolve("patches.jar")
    protected val integrations = directory.resolve("integrations.apk")

    /**
     * Returns true if the bundle has been downloaded to local storage.
     */
    fun hasInstalled() = patchesJar.exists()

    protected suspend fun getVersion() = configRepository.getVersion(id)
    protected suspend fun saveVersion(patches: String, integrations: String) =
        configRepository.updateVersion(id, patches, integrations)

    // TODO: Communicate failure states better.
    protected fun loadBundle(onFail: (Throwable) -> Unit = ::logError) = if (!hasInstalled()) emptyPatchBundle
    else try {
        PatchBundle(patchesJar, integrations.takeIf { it.exists() })
    } catch (err: Throwable) {
        onFail(err)
        emptyPatchBundle
    }

    protected val _bundle = MutableStateFlow(loadBundle())
    val bundle = _bundle.asStateFlow()
}