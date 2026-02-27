package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.installer.RootServiceException
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsViewModel(
    private val installedAppsRepository: InstalledAppRepository,
    private val pm: PM,
    private val rootInstaller: RootInstaller
) : ViewModel() {
    val apps = installedAppsRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    val packageInfoMap = mutableStateMapOf<String, PackageInfo?>()

    init {
        viewModelScope.launch {
            apps.filterNotNull().collectLatest(::fetchPackageInfos)
        }
    }

    private suspend fun fetchPackageInfos(apps: List<InstalledApp>) {
        for (app in apps) {
            packageInfoMap[app.currentPackageName] = withContext(Dispatchers.IO) {
                try {
                    if (app.installType == InstallType.MOUNT &&
                        !rootInstaller.isAppInstalled(app.currentPackageName)
                    ) {
                        installedAppsRepository.delete(app)
                        return@withContext null
                    }
                } catch (_: RootServiceException) {
                }

                val packageInfo = pm.getPackageInfo(app.currentPackageName)

                if (packageInfo == null && app.installType != InstallType.MOUNT) {
                    installedAppsRepository.delete(app)
                    return@withContext null
                }

                packageInfo
            }
        }
    }
}