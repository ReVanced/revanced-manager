package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ManagerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.revanced.manager.util.PM
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.withContext
import java.io.File

class UpdateProgressViewModel(
    app: Application,
    private val managerAPI: ManagerAPI,
    private val pm: PM
) : ViewModel() {

    val downloadProgress by derivedStateOf { managerAPI.downloadProgress?.times(100) ?: 0f }
    val downloadedSize by derivedStateOf { managerAPI.downloadedSize ?: 0L }
    val totalSize by derivedStateOf { managerAPI.totalSize ?: 0L }
    val isInstalling by derivedStateOf { downloadProgress >= 100 }
    var finished by mutableStateOf(false)
        private set

    private val location = File.createTempFile("updater", ".apk", app.cacheDir)
    private val job = viewModelScope.launch {
        uiSafe(app, R.string.download_manager_failed, "Failed to download manager") {
            withContext(Dispatchers.IO) {
                managerAPI.downloadManager(location)
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
