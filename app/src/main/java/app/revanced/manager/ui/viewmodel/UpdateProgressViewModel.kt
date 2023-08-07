package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.api.ReVancedAPI.Extensions.findAssetByType
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.APK_MIMETYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.revanced.manager.util.PM
import app.revanced.manager.util.uiSafe
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.url
import kotlinx.coroutines.withContext
import java.io.File

class UpdateProgressViewModel(
    app: Application,
    private val reVancedAPI: ReVancedAPI,
    private val http: HttpService,
    private val pm: PM
) : ViewModel() {
    var downloadedSize by mutableStateOf(0L)
        private set
    var totalSize by mutableStateOf(0L)
        private set
    val downloadProgress by derivedStateOf {
        if (downloadedSize == 0L || totalSize == 0L) return@derivedStateOf 0f

        downloadedSize.toFloat() / totalSize.toFloat()
    }
    val isInstalling by derivedStateOf { downloadProgress >= 1 }
    var finished by mutableStateOf(false)
        private set

    private val location = File.createTempFile("updater", ".apk", app.cacheDir)
    private val job = viewModelScope.launch {
        uiSafe(app, R.string.download_manager_failed, "Failed to download manager") {
            withContext(Dispatchers.IO) {
                val asset = reVancedAPI
                    .getRelease("revanced-manager")
                    .getOrThrow()
                    .findAssetByType(APK_MIMETYPE)

                http.download(location) {
                    url(asset.downloadUrl)
                    onDownload { bytesSentTotal, contentLength ->
                        downloadedSize = bytesSentTotal
                        totalSize = contentLength
                    }
                }
            }
            finished = true
        }
    }

    fun installUpdate() = viewModelScope.launch {
        pm.installApp(listOf(location))
    }

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        location.delete()
    }
}
