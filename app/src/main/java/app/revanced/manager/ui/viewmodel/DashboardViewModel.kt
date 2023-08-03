package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.PatchBundleRepository
import io.ktor.http.Url
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    app: Application,
    private val patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    val availablePatches =
        patchBundleRepository.bundles.map { it.values.sumOf { bundle -> bundle.patches.size } }
    private val contentResolver: ContentResolver = app.contentResolver

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
}