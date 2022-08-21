package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import app.revanced.manager.preferences.PreferencesManager
import app.revanced.manager.util.ghOrganization
import app.revanced.manager.util.openUrl

class SettingsViewModel(
    private val app: Application,
    val prefs: PreferencesManager
) : ViewModel() {
    fun openGitHub() = app.openUrl(ghOrganization)
}