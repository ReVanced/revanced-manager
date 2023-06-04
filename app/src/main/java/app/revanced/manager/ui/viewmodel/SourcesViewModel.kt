package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.util.uiSafe
import io.ktor.http.*
import kotlinx.coroutines.launch

class SourcesViewModel(private val app: Application, private val sourceRepository: SourceRepository) : ViewModel() {
    val sources = sourceRepository.sources
    private val contentResolver: ContentResolver = app.contentResolver

    companion object {
        const val failLogMsg = "Failed to update patch bundle(s)"
    }

    fun redownloadAllSources() = viewModelScope.launch {
        uiSafe(app, R.string.source_download_fail, failLogMsg) {
            sourceRepository.redownloadRemoteSources()
        }
    }

    suspend fun addLocal(name: String, patchBundle: Uri, integrations: Uri?) {
        contentResolver.openInputStream(patchBundle)!!.use { patchesStream ->
            val integrationsStream = integrations?.let { contentResolver.openInputStream(it) }
            try {
                sourceRepository.createLocalSource(name, patchesStream, integrationsStream)
            } finally {
                integrationsStream?.close()
            }
        }
    }

    suspend fun addRemote(name: String, apiUrl: Url) = sourceRepository.createRemoteSource(name, apiUrl)

    fun deleteSource(source: Source) = viewModelScope.launch { sourceRepository.remove(source) }

    fun deleteAllSources() = viewModelScope.launch {
        sourceRepository.resetConfig()
    }
}