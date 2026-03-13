package app.revanced.manager.domain.sources

import androidx.compose.runtime.Stable
import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

fun interface Loader<T> {
    fun load(file: File): T
}

typealias PatchBundleSource = Source<PatchBundle>

/**
 * A [PatchBundle] or [app.revanced.manager.downloader.Downloader] source.
 */
@Stable
sealed class Source<T>(
    val name: String,
    val uid: Int,
    error: Throwable?,
    protected val file: File,
    protected val loader: Loader<T>
) {
    val state = when {
        error != null -> State.Failed(error)
        !hasInstalled() -> State.Missing
        else -> State.Available(loader.load(file))
    }

    val isDefault inline get() = uid == 0
    val loaded get() = @Suppress("UNCHECKED_CAST") (state as? State.Available<T>)?.obj
    val error get() = (state as? State.Failed)?.throwable

    suspend fun ActionContext.deleteLocalFile() = withContext(Dispatchers.IO) {
        file.delete()
    }

    abstract fun copy(error: Throwable? = this.error, name: String = this.name): Source<T>

    protected fun hasInstalled() = file.exists()

    protected fun outputStream(): OutputStream = with(file) {
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
        data class Available<T>(val obj: T) : State
    }
}

object Extensions {
    val PatchBundleSource.asRemoteOrNull inline get() = this as? RemotePatchBundle
    val PatchBundleSource.version get() = loaded?.manifestAttributes?.version
    val PatchBundleSource.isNameOutOfDate get() = loaded?.manifestAttributes?.name?.let { it != name } == true
}