package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
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
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val reVancedAPI: ReVancedAPI,
    private val networkInfo: NetworkInfo,
    val prefs: PreferencesManager
) : ViewModel() {
    val availablePatches =
        patchBundleRepository.bundles.map { it.values.sumOf { bundle -> bundle.patches.size } }
    private val contentResolver: ContentResolver = app.contentResolver
    private val powerManager = app.getSystemService<PowerManager>()!!
    val sources = patchBundleRepository.sources
    val selectedSources = mutableStateListOf<PatchBundleSource>()

    var updatedManagerVersion: String? by mutableStateOf(null)
        private set
    var showBatteryOptimizationsWarning by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            checkForManagerUpdates()
            showBatteryOptimizationsWarning =
                !powerManager.isIgnoringBatteryOptimizations(app.packageName)
        }
    }

    fun dismissUpdateDialog() {
        updatedManagerVersion = null
    }

    private suspend fun checkForManagerUpdates() {
        if (!prefs.managerAutoUpdates.get() || !networkInfo.isConnected()) return

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            updatedManagerVersion = reVancedAPI.getAppUpdate()?.version
        }
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


    fun cancelSourceSelection() {
        selectedSources.clear()
    }

    fun createLocalSource(patchBundle: Uri, integrations: Uri?) =
        viewModelScope.launch {
            contentResolver.openInputStream(patchBundle)!!.use { patchesStream ->
                integrations?.let { contentResolver.openInputStream(it) }
                    .use { integrationsStream ->
                        patchBundleRepository.createLocal(patchesStream, integrationsStream)
                    }
            }
        }

    fun createRemoteSource(apiUrl: String, autoUpdate: Boolean) =
        viewModelScope.launch { patchBundleRepository.createRemote(apiUrl, autoUpdate) }

    fun delete(bundle: PatchBundleSource) =
        viewModelScope.launch { patchBundleRepository.remove(bundle) }

    fun update(bundle: PatchBundleSource) = viewModelScope.launch {
        if (bundle !is RemotePatchBundle) return@launch

        uiSafe(
            app,
            R.string.source_download_fail,
            RemotePatchBundle.updateFailMsg
        ) {
            if (bundle.update())
                app.toast(app.getString(R.string.bundle_update_success, bundle.getName()))
            else
                app.toast(app.getString(R.string.bundle_update_unavailable, bundle.getName()))
        }
    }
}