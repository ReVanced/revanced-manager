package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.installer.RootServiceException
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.collectEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsViewModel(
    private val installedAppsRepository: InstalledAppRepository,
    private val pm: PM,
    private val rootInstaller: RootInstaller
) : ViewModel() {
    val apps = installedAppsRepository.getAll().flowOn(Dispatchers.IO)

    val packageInfoMap = mutableStateMapOf<String, PackageInfo?>()

    init {
        viewModelScope.launch {
            apps.collectEach { installedApp ->
                packageInfoMap[installedApp.currentPackageName] = withContext(Dispatchers.IO) {
                    try {
                        if (
                            installedApp.installType == InstallType.ROOT && !rootInstaller.isAppInstalled(installedApp.currentPackageName)
                        ) {
                            installedAppsRepository.delete(installedApp)
                            return@withContext null
                        }
                    } catch (_: RootServiceException) {  }

                    val packageInfo = pm.getPackageInfo(installedApp.currentPackageName)

                    if (packageInfo == null && installedApp.installType != InstallType.ROOT) {
                        installedAppsRepository.delete(installedApp)
                        return@withContext null
                    }

                    packageInfo
                }
            }
        }
    }
}