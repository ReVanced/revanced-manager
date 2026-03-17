package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.util.SupportedLocales
import app.revanced.manager.util.resetListItemColorsCached
import kotlinx.coroutines.launch
import java.util.Locale

class GeneralSettingsViewModel(
    private val app: Application,
    val prefs: PreferencesManager
) : ViewModel() {
    fun setTheme(theme: Theme) = viewModelScope.launch {
        prefs.theme.update(theme)
    }

    fun getSupportedLocales() = SupportedLocales.getSupportedLocales(app)
    fun getCurrentLocale() = SupportedLocales.getCurrentLocale()
    fun setLocale(locale: Locale?) = SupportedLocales.setLocale(locale)
    fun getLocaleDisplayName(locale: Locale) = SupportedLocales.getDisplayName(locale)
}