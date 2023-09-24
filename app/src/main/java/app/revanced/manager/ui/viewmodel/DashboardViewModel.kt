package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Log
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

    fun getLegacySettings() {
        val contentResolver = contentResolver
        val contentProviderUri = Uri.parse("content://app.revanced.manager.flutter.provider/settings")
        val cursor: Cursor? = contentResolver.query(contentProviderUri, null, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val column = cursor.getColumnIndex("settings")
                if (column != -1) {
                    val jsonData = cursor.getString(column)
                    // Process the JSON data as needed

                    Log.d("Settings", jsonData)
                }
            }
            cursor.close()
        }
    }
}