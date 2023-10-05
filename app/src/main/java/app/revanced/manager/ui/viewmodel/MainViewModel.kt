package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.asRemoteOrNull
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.network.dto.LegacySettings
import app.revanced.manager.ui.theme.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val patchSelectionRepository: PatchSelectionRepository,
    private val keystoreManager: KeystoreManager,
    val prefs: PreferencesManager
) : ViewModel() {
    lateinit var launcher: ActivityResultLauncher<Intent>

    fun applyAutoUpdatePrefs(manager: Boolean, patches: Boolean) = viewModelScope.launch {
        prefs.showAutoUpdatesDialog.update(false)

        prefs.managerAutoUpdates.update(manager)
        if (patches) {
            with(patchBundleRepository) {
                sources
                    .first()
                    .find { it.uid == 0 }
                    ?.asRemoteOrNull
                    ?.setAutoUpdate(true)

                updateCheck()
            }
        }
    }

    fun launchLegacyActivity(): Boolean {
        return try {
            val intent = Intent()
            intent.setClassName(
                "app.revanced.manager.flutter",
                "app.revanced.manager.flutter.ExportSettingsActivity"
            )
            launcher.launch(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun applyLegacySettings(data: String) = viewModelScope.launch {
        val json = Json { ignoreUnknownKeys = true }
        val settings = json.decodeFromString<LegacySettings>(data)

        settings.themeMode?.let { theme ->
            val themeMap = mapOf(
                0 to Theme.SYSTEM,
                1 to Theme.LIGHT,
                2 to Theme.DARK
            )
            prefs.theme.update(themeMap[theme]!!)
        }
        settings.useDynamicTheme?.let { dynamicColor ->
            prefs.dynamicColor.update(dynamicColor)
        }
        settings.apiUrl?.let { api ->
            prefs.api.update(api.removeSuffix("/"))
        }
        settings.experimentalPatchesEnabled?.let { allowExperimental ->
            prefs.allowExperimental.update(allowExperimental)
        }
        settings.patchesAutoUpdate?.let { autoUpdate ->
            with(patchBundleRepository) {
                sources
                    .first()
                    .find { it.uid == 0 }
                    ?.asRemoteOrNull
                    ?.setAutoUpdate(autoUpdate)

                updateCheck()
            }
        }
        settings.patchesChangeEnabled?.let { disableSelectionWarning ->
            prefs.disableSelectionWarning.update(disableSelectionWarning)
        }
        settings.keystore?.let { keystore ->
            val keystoreBytes = Base64.decode(keystore, Base64.DEFAULT)                                  
            keystoreManager.import(
                "ReVanced",
                settings.keystorePassword,
                keystoreBytes.inputStream()
            )
        }
        settings.patches?.let { selection ->
            patchSelectionRepository.import(0, selection)
        }
        prefs.showAutoUpdatesDialog.update(false)
    }
}
