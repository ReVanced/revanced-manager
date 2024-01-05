package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe

class UpdatesSettingsViewModel(
    prefs: PreferencesManager,
    private val app: Application,
    private val reVancedAPI: ReVancedAPI,
) : ViewModel() {
    val managerAutoUpdates = prefs.managerAutoUpdates

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