package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe

class UpdatesSettingsViewModel(
    prefs: PreferencesManager,
    private val app: Application,
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo,
) : ViewModel() {
    val managerAutoUpdates = prefs.managerAutoUpdates
    val showManagerUpdateDialogOnLaunch = prefs.showManagerUpdateDialogOnLaunch
    val useManagerPrereleases = prefs.useManagerPrereleases

    val isConnected: Boolean
        get() = network.isConnected()

    suspend fun checkForUpdates(): String? {
        var availableVersion: String? = null

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            val update = reVancedAPI.getAppUpdate()
            availableVersion = update?.version

            if (update == null)
                app.toast(app.getString(R.string.no_update_available))
        }

        return availableVersion
    }
}