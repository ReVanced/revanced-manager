package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.service.UninstallService
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InstalledAppInfoViewModel(
    val installedApp: InstalledApp
) : ViewModel(), KoinComponent {
    private val app: Application by inject()
    private val pm: PM by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    val rootInstaller: RootInstaller by inject()

    lateinit var onBackClick: () -> Unit

    var appInfo: PackageInfo? by mutableStateOf(null)
        private set
    var appliedPatches: PatchesSelection? by mutableStateOf(null)
    var isMounted by mutableStateOf(rootInstaller.isAppMounted(installedApp.currentPackageName))
        private set

    fun launch() = pm.launch(installedApp.currentPackageName)

    fun mountOrUnmount() {
        try {
            if (isMounted)
                rootInstaller.unmount(installedApp.currentPackageName)
            else
                rootInstaller.mount(installedApp.currentPackageName)
        } catch (e: Exception) {
            if (isMounted) {
                app.toast(app.getString(R.string.failed_to_unmount, e.simpleMessage()))
                Log.e(tag, "Failed to unmount", e)
            } else {
                app.toast(app.getString(R.string.failed_to_mount, e.simpleMessage()))
                Log.e(tag, "Failed to mount", e)
            }
        } finally {
            isMounted = rootInstaller.isAppMounted(installedApp.currentPackageName)
        }
    }

    fun uninstall() {
        when (installedApp.installType) {
            InstallType.DEFAULT -> pm.uninstallPackage(installedApp.currentPackageName)

            InstallType.ROOT -> viewModelScope.launch {
                rootInstaller.uninstall(installedApp.currentPackageName)
                installedAppRepository.delete(installedApp)
                onBackClick()
            }
        }
    }

    private val uninstallBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UninstallService.APP_UNINSTALL_ACTION -> {
                    val extraStatus =
                        intent.getIntExtra(UninstallService.EXTRA_UNINSTALL_STATUS, -999)
                    val extraStatusMessage =
                        intent.getStringExtra(UninstallService.EXTRA_UNINSTALL_STATUS_MESSAGE)

                    if (extraStatus == PackageInstaller.STATUS_SUCCESS) {
                        viewModelScope.launch {
                            installedAppRepository.delete(installedApp)
                            onBackClick()
                        }
                    } else if (extraStatus != PackageInstaller.STATUS_FAILURE_ABORTED) {
                        app.toast(app.getString(R.string.uninstall_app_fail, extraStatusMessage))
                    }

                }
            }
        }
    }

    init {
        viewModelScope.launch {
            appInfo = withContext(Dispatchers.IO) {
                pm.getPackageInfo(installedApp.currentPackageName)
            }
        }

        viewModelScope.launch {
            appliedPatches = withContext(Dispatchers.IO) {
                installedAppRepository.getAppliedPatches(installedApp.currentPackageName)
            }
        }

        ContextCompat.registerReceiver(
            app,
            uninstallBroadcastReceiver,
            IntentFilter(UninstallService.APP_UNINSTALL_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(uninstallBroadcastReceiver)
    }
}