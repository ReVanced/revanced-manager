package app.revanced.manager.domain.bundles

import androidx.compose.runtime.Stable
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * A [PatchBundle] source.
 */
@Stable
sealed class PatchBundleSource(
    val name: String,
    val uid: Int,
    error: Throwable?,
    protected val directory: File
) {
    protected val patchesFile = directory.resolve("patches.jar")

    val state = when {
        error != null -> State.Failed(error)
        !hasInstalled() -> State.Missing
        else -> State.Available(PatchBundle(patchesFile.absolutePath))
    }

    val patchBundle get() = (state as? State.Available)?.bundle
    val version get() = patchBundle?.manifestAttributes?.version
    val isNameOutOfDate get() = patchBundle?.manifestAttributes?.name?.let { it != name } == true
    val error get() = (state as? State.Failed)?.throwable

    suspend fun ActionContext.deleteLocalFile() = withContext(Dispatchers.IO) {
        patchesFile.delete()
    }

    abstract fun copy(error: Throwable? = this.error, name: String = this.name): PatchBundleSource

    protected fun hasInstalled() = patchesFile.exists()

    protected fun patchBundleOutputStream(): OutputStream = with(patchesFile) {
        // Android 14+ requires dex containers to be readonly.
        try {
            setWritable(true, true)
            outputStream()
        } finally {
            setReadOnly()
        }
    }

    sealed interface State {
        data object Missing : State
        data class Failed(val throwable: Throwable) : State
        data class Available(val bundle: PatchBundle) : State
    }

    companion object Extensions {
        val PatchBundleSource.isDefault inline get() = uid == 0
        val PatchBundleSource.asRemoteOrNull inline get() = this as? RemotePatchBundle
    }
}