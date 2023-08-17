package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.collectEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsViewModel(
    private val installedAppsRepository: InstalledAppRepository,
    private val pm: PM
) : ViewModel() {
    val apps = installedAppsRepository.getAll().flowOn(Dispatchers.IO)

    val packageInfoMap = mutableStateMapOf<String, PackageInfo?>()

    init {
        viewModelScope.launch {
            apps.collectEach { installedApp ->
                packageInfoMap[installedApp.currentPackageName] = withContext(Dispatchers.IO) {
                    pm.getPackageInfo(installedApp.currentPackageName)
                        .also { if (it == null) installedAppsRepository.delete(installedApp) }
                }
            }
        }
    }
}