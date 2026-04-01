package app.revanced.manager.domain.manager

import android.content.Context
import app.revanced.manager.domain.manager.base.BasePreferencesManager
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.util.isDebuggable

class PreferencesManager(
    context: Context
) : BasePreferencesManager(context, "settings") {
    val dynamicColor = booleanPreference("dynamic_color", true)
    val pureBlackTheme = booleanPreference("pure_black_theme", false)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val api = stringPreference("api_url", "https://api.revanced.app")

    val useProcessRuntime = booleanPreference("use_process_runtime", false)
    val patcherProcessMemoryLimit = intPreference("process_runtime_memory_limit", 700)

    val keystoreAlias = stringPreference("keystore_alias", KeystoreManager.DEFAULT)
    val keystorePass = stringPreference("keystore_pass", KeystoreManager.DEFAULT)

    val completedOnboarding = booleanPreference("completed_onboarding", false)
    val readAnnouncements = longSetPreference("read_announcements", emptySet())
    val selectedAnnouncementTags = stringSetPreference("selected_announcement_tags", setOf("revanced", "manager"))
    val managerAutoUpdates = booleanPreference("manager_auto_updates", false)
    val showManagerUpdateDialogOnLaunch = booleanPreference("show_manager_update_dialog_on_launch", true)
    val useManagerPrereleases = booleanPreference("manager_prereleases", false)
    val usePatchesPrereleases = booleanPreference("patches_prereleases", false)
    val useDownloaderPrerelease = booleanPreference("downloader_prereleases", false)

    val disablePatchVersionCompatCheck = booleanPreference("disable_patch_version_compatibility_check", false)
    val disableSelectionWarning = booleanPreference("disable_selection_warning", false)
    val disableUniversalPatchCheck = booleanPreference("disable_patch_universal_check", false)
    val suggestedVersionSafeguard = booleanPreference("suggested_version_safeguard", true)

    val showDeveloperSettings = booleanPreference("show_developer_settings", context.isDebuggable)

    val allowMeteredNetworks = booleanPreference("allow_metered_networks", false)

    val pinnedApps = stringSetPreference("pinned_apps", emptySet())
}
