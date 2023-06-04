package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.theme.Theme

class SettingsViewModel(
    val prefs: PreferencesManager
) : ViewModel() {

    fun setTheme(theme: Theme) {
        prefs.theme = theme
    }

}