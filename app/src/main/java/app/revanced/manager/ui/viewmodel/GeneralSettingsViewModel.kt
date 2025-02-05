package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.theme.Theme
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    val prefs: PreferencesManager
) : ViewModel() {
    fun setTheme(theme: Theme) = viewModelScope.launch {
        prefs.theme.update(theme)
    }
}