package app.revanced.manager.ui.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.network.api.ManagerAPI
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class UpdateSettingsViewModel(
    private val managerAPI: ManagerAPI,
    private val pm: PM
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
        pm.installApp(
            apks = listOf(
                File(
                    (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/revanced-manager.apk")
                        .toString())
                ),
            )
        )
    }

    init {
        downloadLatestManager()
    }
}