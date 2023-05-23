package app.revanced.manager.compose.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.compose.network.api.ManagerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.revanced.manager.compose.util.PM
import java.io.File

class UpdateSettingsViewModel(
    private val managerAPI: ManagerAPI,
    private val app: Application,
) : ViewModel() {
    val downloadProgress get() = (managerAPI.downloadProgress?.times(100)) ?: 0f
    val downloadedSize get() = managerAPI.downloadedSize ?: 0L
    val totalSize get() = managerAPI.totalSize ?: 0L
    private fun downloadLatestManager() {
        viewModelScope.launch(Dispatchers.IO) {
            managerAPI.downloadManager()
        }
    }
    fun installUpdate() {
        PM.installApp(
            apks = listOf(
                File(
                    (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/revanced-manager.apk")
                        .toString())
                ),
            ),
            context = app,
        )
    }


    init {
        downloadLatestManager()
    }
}