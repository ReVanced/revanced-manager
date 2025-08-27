package app.revanced.manager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.PM
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val downloaderPluginRepository: DownloaderPluginRepository,
    private val reVancedAPI: ReVancedAPI,
    private val networkInfo: NetworkInfo,
    val prefs: PreferencesManager,
    private val pm: PM,
) : ViewModel() {
    val availablePatches =
        patchBundleRepository.bundleInfoFlow.map { it.values.sumOf { bundle -> bundle.patches.size } }
    private val contentResolver: ContentResolver = app.contentResolver
    private val powerManager = app.getSystemService<PowerManager>()!!

    val newDownloaderPluginsAvailable =
        downloaderPluginRepository.newPluginPackageNames.map { it.isNotEmpty() }

    /**
     * Android 11 kills the app process after granting the "install apps" permission, which is a problem for the patcher screen.
     * This value is true when the conditions that trigger the bug are met.
     *
     * See: https://github.com/ReVanced/revanced-manager/issues/2138
     */
    val android11BugActive get() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !pm.canInstallPackages()

    var updatedManagerVersion: String? by mutableStateOf(null)
        private set
    var showBatteryOptimizationsWarning by mutableStateOf(false)
        private set

    private val bundleListEventsChannel = Channel<BundleListViewModel.Event>()
    val bundleListEventsFlow = bundleListEventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            checkForManagerUpdates()
            updateBatteryOptimizationsWarning()
        }
    }

    fun ignoreNewDownloaderPlugins() = viewModelScope.launch {
        downloaderPluginRepository.acknowledgeAllNewPlugins()
    }

    private suspend fun checkForManagerUpdates() {
        if (!prefs.managerAutoUpdates.get() || !networkInfo.isConnected()) return

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            updatedManagerVersion = reVancedAPI.getAppUpdate()?.version
        }
    }

    fun updateBatteryOptimizationsWarning() {
        showBatteryOptimizationsWarning =
            !powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun setShowManagerUpdateDialogOnLaunch(value: Boolean) {
        viewModelScope.launch {
            prefs.showManagerUpdateDialogOnLaunch.update(value)
        }
    }

    fun applyAutoUpdatePrefs(manager: Boolean, patches: Boolean) = viewModelScope.launch {
        prefs.firstLaunch.update(false)

        prefs.managerAutoUpdates.update(manager)

        if (manager) checkForManagerUpdates()

        if (patches) {
            with(patchBundleRepository) {
                sources
                    .first()
                    .find { it.uid == 0 }
                    ?.asRemoteOrNull
                    ?.setAutoUpdate(true)

                updateCheck()
            }
        }
    }

    private fun sendEvent(event: BundleListViewModel.Event) {
        viewModelScope.launch { bundleListEventsChannel.send(event) }
    }

    fun cancelSourceSelection() = sendEvent(BundleListViewModel.Event.CANCEL)
    fun updateSources() = sendEvent(BundleListViewModel.Event.UPDATE_SELECTED)
    fun deleteSources() = sendEvent(BundleListViewModel.Event.DELETE_SELECTED)

    @SuppressLint("Recycle")
    fun createLocalSource(patchBundle: Uri) = viewModelScope.launch {
        patchBundleRepository.createLocal { contentResolver.openInputStream(patchBundle)!! }
    }

    fun createRemoteSource(apiUrl: String, autoUpdate: Boolean) = viewModelScope.launch {
        patchBundleRepository.createRemote(apiUrl, autoUpdate)
    }
}