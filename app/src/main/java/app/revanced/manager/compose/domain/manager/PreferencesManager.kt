package app.revanced.manager.compose.domain.manager

import android.content.SharedPreferences
import app.revanced.manager.compose.domain.manager.base.BasePreferenceManager
import app.revanced.manager.compose.ui.theme.Theme

/**
 * @author Hyperion Authors, zt64
 */
class PreferencesManager(
    sharedPreferences: SharedPreferences
) : BasePreferenceManager(sharedPreferences) {
    var dynamicColor by booleanPreference("dynamic_color", true)
    var theme by enumPreference("theme", Theme.SYSTEM)
    //var sentry by booleanPreference("sentry", true)
}
