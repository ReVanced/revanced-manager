package app.revanced.manager.domain.bundles

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.repository.PatchBundlePersistenceRepository
import app.revanced.manager.patcher.patch.PatchBundle
import app.revanced.manager.util.tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.OutputStream

/**
 * A [PatchBundle] source.
 */
@Stable
sealed class PatchBundleSource(initialName: String, val uid: Int, directory: File) : KoinComponent {
    protected val configRepository: PatchBundlePersistenceRepository by inject()
    private val app: Application by inject()
    protected val patchesFile = directory.resolve("patches.jar")
    protected val integrationsFile = directory.resolve("integrations.apk")

    private val _state = MutableStateFlow(load())
    val state = _state.asStateFlow()

    private val _nameFlow = MutableStateFlow(initialName)
    val nameFlow =
        _nameFlow.map { it.ifEmpty { app.getString(if (isDefault) R.string.bundle_name_default else R.string.bundle_name_fallback) } }

    suspend fun getName() = nameFlow.first()

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

    private fun load(): State {
        if (!hasInstalled()) return State.Missing

        return try {
            State.Loaded(PatchBundle(patchesFile, integrationsFile.takeIf(File::exists)))
        } catch (t: Throwable) {
            Log.e(tag, "Failed to load patch bundle with UID $uid", t)
            State.Failed(t)
        }
    }

    suspend fun reload(): PatchBundle? {
        val newState = load()
        _state.value = newState

        val bundle = newState.patchBundleOrNull()
        // Try to read the name from the patch bundle manifest if the bundle does not have a name.
        if (bundle != null && _nameFlow.value.isEmpty()) {
            bundle.readManifestAttribute("Name")?.let { setName(it) }
        }

        return bundle
    }

    /**
     * Create a flow that emits the [app.revanced.manager.data.room.bundles.BundleProperties] of this [PatchBundleSource].
     * The flow will emit null if the associated [PatchBundleSource] is deleted.
     */
    fun propsFlow() = configRepository.getProps(uid).flowOn(Dispatchers.Default)
    suspend fun getProps() = propsFlow().first()!!

    suspend fun currentVersion() = getProps().versionInfo
    protected suspend fun saveVersion(patches: String?, integrations: String?) =
        configRepository.updateVersion(uid, patches, integrations)

    suspend fun setName(name: String) {
        configRepository.setName(uid, name)
        _nameFlow.value = name
    }

    sealed interface State {
        fun patchBundleOrNull(): PatchBundle? = null

        data object Missing : State
        data class Failed(val throwable: Throwable) : State
        data class Loaded(val bundle: PatchBundle) : State {
            override fun patchBundleOrNull() = bundle
        }
    }

    companion object Extensions {
        val PatchBundleSource.isDefault inline get() = uid == 0
        val PatchBundleSource.asRemoteOrNull inline get() = this as? RemotePatchBundle
        val PatchBundleSource.nameState
            @Composable inline get() = nameFlow.collectAsStateWithLifecycle(
                ""
            )
    }
}