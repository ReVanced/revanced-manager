package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.ManagerUpdateRepository
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe

class UpdatesSettingsViewModel(
    prefs: PreferencesManager,
    private val app: Application,
    private val managerUpdateRepository: ManagerUpdateRepository,
    private val network: NetworkInfo,
) : ViewModel() {
    val managerAutoUpdates = prefs.managerAutoUpdates
    val downloaderAutoUpdates = prefs.downloaderAutoUpdates
    val showManagerUpdateDialogOnLaunch = prefs.showManagerUpdateDialogOnLaunch
    val useManagerPrereleases = prefs.useManagerPrereleases
    val availableManagerUpdate = managerUpdateRepository.availableVersion

    val isConnected: Boolean
        get() = network.isConnected()

    suspend fun checkForUpdates(): String? {
        var availableVersion: String? = null

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            availableVersion = managerUpdateRepository.refreshAvailableVersion()

            if (availableVersion == null)
                app.toast(app.getString(R.string.no_update_available))
        }

        return availableVersion
    }

    fun clearAvailableManagerUpdate() {
        managerUpdateRepository.clearAvailableVersion()
    }
}