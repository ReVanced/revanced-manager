package app.revanced.manager.manager

import android.content.SharedPreferences
import app.revanced.manager.manager.base.BasePreferenceManager

class PreferencesManager(
    sharedPreferences: SharedPreferences
) : BasePreferenceManager(sharedPreferences) {
    var dynamicColor by booleanPreference("dynamic_color", false)
    var theme by booleanPreference("theme", false)
    var midnightMode by booleanPreference("midnight_mode", false)
    var autoUpdate by booleanPreference("auto_update", false)
}