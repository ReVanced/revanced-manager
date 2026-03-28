package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.installer.RootServiceException
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.AppInfo
import app.revanced.manager.util.PM
import app.revanced.manager.util.isSystemApp
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

data class AppInfoState(
    val packageName: String,
    val label: String,
    val isPatched: Boolean,
    val isInstalled: Boolean,
    val patchCount: Int,
    val suggestedVersion: String?,
    val packageInfo: PackageInfo?,
    val installedApp: InstalledApp? = null
)

@OptIn(SavedStateHandleSaveableApi::class)
class AppsViewModel(
    private val app: Application,
    private val installedAppsRepository: InstalledAppRepository,
    private val pm: PM,
    private val rootInstaller: RootInstaller,
    private val prefs: PreferencesManager,
    fs: Filesystem,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val inputFile = savedStateHandle.saveable(key = "inputFile") {
        File(fs.uiTempDir, "input.apk").also(File::delete)
    }

    val filterTextFlow = MutableStateFlow("")

    val patchableApps = pm.appList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    val installedApps = installedAppsRepository.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    val pinnedApps = pm.pinnedApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptySet(),
    )

    val suggestedVersions = pm.suggestedVersions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyMap(),
    )

    val packageInfoMap = mutableStateMapOf<String, PackageInfo?>()
    private val patchedLabelCache = mutableMapOf<String, String>()

    val showPatched = prefs.showPatched.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val showInstalled = prefs.showInstalled.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val showNotInstalled = prefs.showNotInstalled.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val showSystem = prefs.showSystem.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val applyFilterToPinned = prefs.applyFilterToPinned.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val completeAppList: StateFlow<List<AppInfoState>?> = combine(
        installedApps,
        patchableApps,
        pinnedApps,
        suggestedVersions,
        showPatched,
        showInstalled,
        showNotInstalled,
        showSystem,
        applyFilterToPinned
    ) { params ->
        val patched = params[0] as? List<InstalledApp>? ?: return@combine null
        val patchable = params[1] as? List<AppInfo>? ?: return@combine null
        val pinned = params[2] as Set<String>
        val suggested = params[3] as Map<String, String>
        val sPatched = params[4] as Boolean
        val sInstalled = params[5] as Boolean
        val sNotInstalled = params[6] as Boolean
        val sSystem = params[7] as Boolean
        val aPinned = params[8] as Boolean

        val patchedPkgNames = patched.flatMap { listOf(it.currentPackageName, it.originalPackageName) }.toSet()
        val patchesData = patchable.associateBy { it.packageName }
        val allApps = mutableListOf<AppInfoState>()

        patched.forEach { app ->
            val packageInfo = packageInfoMap[app.currentPackageName]
            val isPinned = app.currentPackageName in pinned || app.originalPackageName in pinned
            val isSystem = packageInfo?.isSystemApp() == true
            val passesFilters = sPatched && (sSystem || !isSystem)

            if (passesFilters || (isPinned && !aPinned)) {
                val pData = patchesData[app.currentPackageName] ?: patchesData[app.originalPackageName]
                val label = patchedLabelCache.getOrPut(app.currentPackageName) { loadLabel(packageInfo) }
                allApps.add(
                    AppInfoState(
                        packageName = app.currentPackageName,
                        label = label,
                        isPatched = true,
                        isInstalled = true,
                        patchCount = pData?.patches ?: 0,
                        suggestedVersion = suggested[app.currentPackageName] ?: suggested[app.originalPackageName],
                        packageInfo = packageInfo,
                        installedApp = app
                    )
                )
            }
        }

        patchable.forEach { app ->
            if (app.packageName !in patchedPkgNames) {
                val isPinned = app.packageName in pinned
                val isInstalled = app.packageInfo != null
                val isSystem = app.packageInfo?.isSystemApp() == true
                val passesFilters = ((isInstalled && sInstalled) || (!isInstalled && sNotInstalled)) && (sSystem || !isSystem)

                if (passesFilters || (isPinned && !aPinned)) {
                    allApps.add(
                        AppInfoState(
                            packageName = app.packageName,
                            label = loadLabel(app.packageInfo),
                            isPatched = false,
                            isInstalled = isInstalled,
                            patchCount = app.patches ?: 0,
                            suggestedVersion = suggested[app.packageName],
                            packageInfo = app.packageInfo
                        )
                    )
                }
            }
        }

        allApps.sortedWith(
            compareByDescending<AppInfoState> { it.isPatched }
                .thenByDescending { it.isInstalled }
                .thenByDescending { it.patchCount }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val filteredAppList: StateFlow<List<AppInfoState>?> = combine(
        completeAppList,
        filterTextFlow
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps?.filter { 
            it.packageName.contains(query, ignoreCase = true) || 
            it.label.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val storageSelectionChannel = Channel<SelectedApp.Local>()
    val storageSelectionFlow = storageSelectionChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            installedApps.filterNotNull().collectLatest(::fetchPackageInfos)
        }
    }

    fun setFilterText(filter: String) {
        filterTextFlow.value = filter
    }

    fun setShowPatched(value: Boolean) = viewModelScope.launch { prefs.showPatched.update(value) }
    fun setShowInstalled(value: Boolean) = viewModelScope.launch { prefs.showInstalled.update(value) }
    fun setShowNotInstalled(value: Boolean) = viewModelScope.launch { prefs.showNotInstalled.update(value) }
    fun setShowSystem(value: Boolean) = viewModelScope.launch { prefs.showSystem.update(value) }
    fun setApplyFilterToPinned(value: Boolean) = viewModelScope.launch { prefs.applyFilterToPinned.update(value) }

    fun loadLabel(app: PackageInfo?) = with(pm) { app?.label() ?: "Not installed" }

    fun handleStorageResult(uri: Uri) = viewModelScope.launch {
        val selectedApp = withContext(Dispatchers.IO) { loadSelectedFile(uri) }

        if (selectedApp == null) {
            app.toast(app.getString(R.string.failed_to_load_apk))
            return@launch
        }

        storageSelectionChannel.send(selectedApp)
    }

    private suspend fun fetchPackageInfos(apps: List<InstalledApp>) {
        for (app in apps) {
            packageInfoMap[app.currentPackageName] = withContext(Dispatchers.IO) {
                if (app.installType == InstallType.MOUNT) {
                    try {
                        if (!rootInstaller.isAppInstalled(app.currentPackageName)) {
                            installedAppsRepository.delete(app)
                            return@withContext null
                        }
                    } catch (_: RootServiceException) { }
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

    fun togglePin(packageName: String) = viewModelScope.launch {
        pm.togglePin(packageName)
    }

    private fun loadSelectedFile(uri: Uri): SelectedApp.Local? =
        app.contentResolver.openInputStream(uri)?.use { stream ->
            inputFile.delete()
            Files.copy(stream, inputFile.toPath())

            pm.getPackageInfo(inputFile)?.let { packageInfo ->
                SelectedApp.Local(
                    packageName = packageInfo.packageName,
                    version = packageInfo.versionName!!,
                    file = inputFile,
                    temporary = true,
                )
            }
        }
}
