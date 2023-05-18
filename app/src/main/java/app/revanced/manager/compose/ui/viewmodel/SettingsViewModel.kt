package app.revanced.manager.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.revanced.manager.compose.domain.manager.PreferencesManager
import app.revanced.manager.compose.ui.theme.Theme

class SettingsViewModel(
    val prefs: PreferencesManager
): ViewModel() {

    fun setTheme(theme: Theme) {
        prefs.theme = theme
    }

}