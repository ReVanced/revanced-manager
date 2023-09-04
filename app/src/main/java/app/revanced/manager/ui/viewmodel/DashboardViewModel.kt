package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    val availablePatches =
        patchBundleRepository.bundles.map { it.values.sumOf { bundle -> bundle.patches.size } }
    private val contentResolver: ContentResolver = app.contentResolver
    val sources = patchBundleRepository.sources
    val selectedSources = mutableStateListOf<PatchBundleSource>()

    fun cancelSourceSelection() {
        selectedSources.clear()
    }
    fun createLocalSource(name: String, patchBundle: Uri, integrations: Uri?) =
        viewModelScope.launch {
            contentResolver.openInputStream(patchBundle)!!.use { patchesStream ->
                val integrationsStream = integrations?.let { contentResolver.openInputStream(it) }
                try {
                    patchBundleRepository.createLocal(name, patchesStream, integrationsStream)
                } finally {
                    integrationsStream?.close()
                }
            }
        }

    fun createRemoteSource(name: String, apiUrl: String, autoUpdate: Boolean) =
        viewModelScope.launch { patchBundleRepository.createRemote(name, apiUrl, autoUpdate) }

    fun delete(bundle: PatchBundleSource) =
        viewModelScope.launch { patchBundleRepository.remove(bundle) }

    fun update(bundle: PatchBundleSource) = viewModelScope.launch {
        if (bundle !is RemotePatchBundle) return@launch

        uiSafe(
            app,
            R.string.source_download_fail,
            RemotePatchBundle.updateFailMsg
        ) {
            bundle.update()
        }
    }
}