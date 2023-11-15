package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.api.ReVancedAPI.Extensions.findAssetByType
import app.revanced.manager.network.dto.ReVancedRelease
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.PM
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class UpdateViewModel(
    private val downloadOnScreenEntry: Boolean
) : ViewModel(), KoinComponent {
    private val app: Application by inject()
    private val reVancedAPI: ReVancedAPI by inject()
    private val http: HttpService by inject()
    private val pm: PM by inject()
    private val networkInfo: NetworkInfo by inject()

    var downloadedSize by mutableStateOf(0L)
        private set
    var totalSize by mutableStateOf(0L)
        private set
    val downloadProgress by derivedStateOf {
        if (downloadedSize == 0L || totalSize == 0L) return@derivedStateOf 0f

        downloadedSize.toFloat() / totalSize.toFloat()
    }
    var showInternetCheckDialog by mutableStateOf(false)
    var state by mutableStateOf(State.CAN_DOWNLOAD)
        private set

    var installError by mutableStateOf("")

    var changelog: Changelog? by mutableStateOf(null)

    private val location = File.createTempFile("updater", ".apk", app.cacheDir)
    private var release: ReVancedRelease? = null
    private val job = viewModelScope.launch {
        uiSafe(app, R.string.download_manager_failed, "Failed to download ReVanced Manager") {
            withContext(Dispatchers.IO) {
                val response = reVancedAPI
                    .getLatestRelease("revanced-manager")
                    .getOrThrow()
                release = response
                changelog = Changelog(
                    response.metadata.tag,
                    response.findAssetByType(APK_MIMETYPE).downloadCount,
                    response.metadata.publishedAt,
                    response.metadata.body
                )
            }
            if (downloadOnScreenEntry) {
                downloadUpdate()
            } else {
                state = State.CAN_DOWNLOAD
            }
        }
    }

    fun downloadUpdate(ignoreInternetCheck: Boolean = false) = viewModelScope.launch {
        uiSafe(app, R.string.failed_to_download_update, "Failed to download update") {
            withContext(Dispatchers.IO) {
                if (!networkInfo.isSafe() && !ignoreInternetCheck) {
                    showInternetCheckDialog = true
                } else {
                    state = State.DOWNLOADING
                    val asset = release?.findAssetByType(APK_MIMETYPE)
                        ?: throw Exception("couldn't find asset to download")

                    http.download(location) {
                        url(asset.downloadUrl)
                        onDownload { bytesSentTotal, contentLength ->
                            downloadedSize = bytesSentTotal
                            totalSize = contentLength
                        }
                    }
                    state = State.CAN_INSTALL
                }
            }
        }
    }

    fun installUpdate() = viewModelScope.launch {
        state = State.INSTALLING

        pm.installApp(listOf(location))
    }

    private val installBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val pmStatus = intent.getIntExtra(InstallService.EXTRA_INSTALL_STATUS, -999)
                val extra =
                    intent.getStringExtra(InstallService.EXTRA_INSTALL_STATUS_MESSAGE)!!

                if (pmStatus == PackageInstaller.STATUS_SUCCESS) {
                    app.toast(app.getString(R.string.install_app_success))
                    state = State.SUCCESS
                } else {
                    state = State.FAILED
                    // TODO: handle install fail with a popup
                    installError = extra
                    app.toast(app.getString(R.string.install_app_fail, extra))
                }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(app, installBroadcastReceiver, IntentFilter().apply {
            addAction(InstallService.APP_INSTALL_ACTION)
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

    enum class State(@StringRes val title: Int, val showCancel: Boolean = false) {
        CAN_DOWNLOAD(R.string.update_available),
        DOWNLOADING(R.string.downloading_manager_update, true),
        CAN_INSTALL(R.string.ready_to_install_update, true),
        INSTALLING(R.string.installing_manager_update),
        FAILED(R.string.install_update_manager_failed),
        SUCCESS(R.string.update_completed)
    }
}
