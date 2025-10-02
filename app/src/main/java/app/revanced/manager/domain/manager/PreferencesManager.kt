package app.revanced.manager.domain.manager

import android.content.Context
import app.revanced.manager.domain.manager.base.BasePreferencesManager
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.util.isDebuggable

class PreferencesManager(
    context: Context
) : BasePreferencesManager(context, "settings") {
    val dynamicColor = booleanPreference("dynamic_color", true)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val api = stringPreference("api_url", "https://api.revanced.app")

    val useProcessRuntime = booleanPreference("use_process_runtime", false)
    val patcherProcessMemoryLimit = intPreference("process_runtime_memory_limit", 700)

    val keystoreAlias = stringPreference("keystore_alias", KeystoreManager.DEFAULT)
    val keystorePass = stringPreference("keystore_pass", KeystoreManager.DEFAULT)

    val firstLaunch = booleanPreference("first_launch", true)
    val managerAutoUpdates = booleanPreference("manager_auto_updates", false)
    val showManagerUpdateDialogOnLaunch = booleanPreference("show_manager_update_dialog_on_launch", true)

    val disablePatchVersionCompatCheck = booleanPreference("disable_patch_version_compatibility_check", false)
    val disableSelectionWarning = booleanPreference("disable_selection_warning", false)
    val disableUniversalPatchCheck = booleanPreference("disable_patch_universal_check", false)
    val suggestedVersionSafeguard = booleanPreference("suggested_version_safeguard", true)

    val acknowledgedDownloaderPlugins = stringSetPreference("acknowledged_downloader_plugins", emptySet())

    val showDeveloperSettings = booleanPreference("show_developer_settings", context.isDebuggable)
}
