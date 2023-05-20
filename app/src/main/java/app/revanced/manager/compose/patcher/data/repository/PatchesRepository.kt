package app.revanced.manager.compose.patcher.data.repository

import android.util.Log
import app.revanced.manager.compose.network.api.ManagerAPI
import app.revanced.manager.compose.patcher.data.PatchBundle
import app.revanced.manager.compose.patcher.patch.PatchInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PatchesRepository(private val managerAPI: ManagerAPI) {
    private val patchInformation =
        MutableSharedFlow<List<PatchInfo>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var bundle: PatchBundle? = null

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    /**
     * Load a new bundle and update state associated with it.
     */
    private suspend fun loadNewBundle(new: PatchBundle) {
        bundle = new
        withContext(Dispatchers.Main) {
            patchInformation.emit(new.loadAllPatches().map { PatchInfo(it) })
        }
    }

    /**
     * Load the [PatchBundle] if needed.
     */
    private suspend fun loadBundle() = bundle ?: PatchBundle(
        managerAPI.downloadPatchBundle()!!.absolutePath,
        managerAPI.downloadIntegrations()
    ).also {
        loadNewBundle(it)
    }

    suspend fun loadPatchClassesFiltered(packageName: String) =
        loadBundle().loadPatchesFiltered(packageName)

    fun getPatchInformation() = patchInformation.asSharedFlow().also {
        scope.launch {
            try {
                loadBundle()
            } catch (e: Throwable) {
                Log.e("revanced-manager", "Failed to download bundle", e)
            }
        }
    }

    suspend fun getIntegrations() = listOfNotNull(loadBundle().integrations)
}