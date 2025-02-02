package app.revanced.manager.domain.manager

import android.content.Context
import app.revanced.manager.domain.manager.base.BasePreferencesManager
import app.revanced.manager.ui.theme.Theme

class PreferencesManager(
    context: Context
) : BasePreferencesManager(context, "settings") {
    val dynamicColor = booleanPreference("dynamic_color", true)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val api = stringPreference("api_url", "https://api.revanced.app")

    val useProcessRuntime = booleanPreference("use_process_runtime", false)
    val patcherProcessMemoryLimit = intPreference("process_runtime_memory_limit", 700)

    val keystoreCommonName = stringPreference("keystore_cn", KeystoreManager.DEFAULT)
    val keystorePass = stringPreference("keystore_pass", KeystoreManager.DEFAULT)

    val firstLaunch = booleanPreference("first_launch", true)
    val managerAutoUpdates = booleanPreference("manager_auto_updates", false)
    val showManagerUpdateDialogOnLaunch = booleanPreference("show_manager_update_dialog_on_launch", true)

    val allowIncompatibleMixing = booleanPreference("allow_incompatible_mixing", false)
    val allowChangingPatchSelection = booleanPreference("allow_changing_patch_selection", false)
    val allowUniversalPatch = booleanPreference("allow_universal_patch", false)

    val acknowledgedDownloaderPlugins = stringSetPreference("acknowledged_downloader_plugins", emptySet())
}
