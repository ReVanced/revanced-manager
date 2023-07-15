package app.revanced.manager.domain.manager

import android.content.Context
import app.revanced.manager.domain.manager.base.BasePreferencesManager
import app.revanced.manager.ui.theme.Theme

class PreferencesManager(
    context: Context
) : BasePreferencesManager(context, "settings") {
    val dynamicColor = booleanPreference("dynamic_color", true)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val allowExperimental = booleanPreference("allow_experimental", false)

    val keystoreCommonName = stringPreference("keystore_cn", KeystoreManager.DEFAULT)
    val keystorePass = stringPreference("keystore_pass", KeystoreManager.DEFAULT)

    val preferSplits = booleanPreference("prefer_splits", false)
}
