package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.SearchForUpdatesBackgroundInterval
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class UpdatesSettingsViewModel(
    val prefs: PreferencesManager,
    private val app: Application,
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo,
    private val workerRepository: WorkerRepository
) : ViewModel() {
    val managerAutoUpdates = prefs.managerAutoUpdates
    val showManagerUpdateDialogOnLaunch = prefs.showManagerUpdateDialogOnLaunch

    fun updateBackgroundBundleUpdateTime(searchForUpdatesBackgroundInterval: SearchForUpdatesBackgroundInterval) {
        viewModelScope.launch {
            prefs.searchForUpdatesBackgroundInterval.update(searchForUpdatesBackgroundInterval)
            workerRepository.scheduleBundleUpdateNotificationWork(searchForUpdatesBackgroundInterval)
        }
    }

    val isConnected: Boolean
        get() = network.isConnected()

    suspend fun checkForUpdates(): Boolean {
        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            app.toast(app.getString(R.string.update_check))

            if (reVancedAPI.getAppUpdate() == null)
                app.toast(app.getString(R.string.no_update_available))
            else
                return true
        }

        return false
    }
}