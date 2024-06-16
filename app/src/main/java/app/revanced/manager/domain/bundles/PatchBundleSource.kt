package app.revanced.manager.domain.bundles

import androidx.compose.runtime.Stable
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.OutputStream

/**
 * A [PatchBundle] source.
 */
@Stable
sealed class PatchBundleSource(val name: String, val uid: Int, directory: File) {
    protected val patchesFile = directory.resolve("patches.jar")
    protected val integrationsFile = directory.resolve("integrations.apk")

    private val _state = MutableStateFlow(getPatchBundle())
    val state = _state.asStateFlow()

    /**
     * Returns true if the bundle has been downloaded to local storage.
     */
    fun hasInstalled() = patchesFile.exists()

    protected fun patchBundleOutputStream(): OutputStream = with(patchesFile) {
        // Android 14+ requires dex containers to be readonly.
        try {
            setWritable(true, true)
            outputStream()
        } finally {
            setReadOnly()
        }
    }

    private fun getPatchBundle() =
        if (!hasInstalled()) State.Missing
        else State.Available(PatchBundle(patchesFile, integrationsFile.takeIf(File::exists)))

    fun refresh() {
        _state.value = getPatchBundle()
    }

    fun markAsFailed(e: Throwable) {
        _state.value = State.Failed(e)
    }

    sealed interface State {
        fun patchBundleOrNull(): PatchBundle? = null

        data object Missing : State
        data class Failed(val throwable: Throwable) : State
        data class Available(val bundle: PatchBundle) : State {
            override fun patchBundleOrNull() = bundle
        }
    }

    companion object {
        val PatchBundleSource.isDefault get() = uid == 0
        val PatchBundleSource.asRemoteOrNull get() = this as? RemotePatchBundle
        fun PatchBundleSource.propsOrNullFlow() = asRemoteOrNull?.propsFlow() ?: flowOf(null)
    }
}