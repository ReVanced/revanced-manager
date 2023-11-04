package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.api.ReVancedAPI.Extensions.findAssetByType
import app.revanced.manager.network.dto.ReVancedRelease
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.util.APK_MIMETYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.revanced.manager.util.PM
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.url
import kotlinx.coroutines.withContext
import java.io.File

class UpdateViewModel(
    private val app: Application,
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
    private val _state = mutableStateOf(State.CAN_DOWNLOAD)
    val state: State get() = _state.value

    var installError by mutableStateOf("")

    var changelog: Changelog? by mutableStateOf(null)

    private val location = File.createTempFile("updater", ".apk", app.cacheDir)
    private var release: ReVancedRelease? = null
    private val job = viewModelScope.launch {
        uiSafe(app, R.string.download_manager_failed, "Failed to download ReVanced Manager") {
            withContext(Dispatchers.IO) {
                release = reVancedAPI
                    .getLatestRelease("revanced-manager")
                    .getOrThrow()
                changelog = Changelog(
                    release!!.metadata.tag,
                    release!!.findAssetByType(APK_MIMETYPE).downloadCount,
                    release!!.metadata.publishedAt,
                    release!!.metadata.body
                )
            }
            _state.value = State.CAN_DOWNLOAD
        }
    }

    fun downloadUpdate() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _state.value = State.DOWNLOADING
            val asset = release!!.findAssetByType(APK_MIMETYPE)

            http.download(location) {
                url(asset.downloadUrl)
                onDownload { bytesSentTotal, contentLength ->
                    downloadedSize = bytesSentTotal
                    totalSize = contentLength
                }
            }
            _state.value = State.CAN_INSTALL
        }
    }

    fun installUpdate() = viewModelScope.launch {
        _state.value = State.INSTALLING

        pm.installApp(listOf(location))
    }

    private val installBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallService.APP_INSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(InstallService.EXTRA_INSTALL_STATUS, -999)
                    val extra = intent.getStringExtra(InstallService.EXTRA_INSTALL_STATUS_MESSAGE)!!

                    if (pmStatus == PackageInstaller.STATUS_SUCCESS) {
                        app.toast(app.getString(R.string.install_app_success))
                        _state.value = State.SUCCESS
                    } else {
                        _state.value = State.FAILED
                        installError = extra
                        app.toast(app.getString(R.string.install_app_fail, extra))
                    }
                }

                UninstallService.APP_UNINSTALL_ACTION -> {
                }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(app, installBroadcastReceiver, IntentFilter().apply {
            addAction(InstallService.APP_INSTALL_ACTION)
            addAction(UninstallService.APP_UNINSTALL_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(installBroadcastReceiver)

        job.cancel()
        location.delete()
    }

    data class Changelog(
        val version: String,
        val downloadCount: Int,
        val publishDate: String,
        val body: String,
    )

    enum class State {
        CAN_DOWNLOAD,
        DOWNLOADING,
        CAN_INSTALL,
        INSTALLING,
        FAILED,
        SUCCESS
    }
}
