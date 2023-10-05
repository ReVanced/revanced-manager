package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.asRemoteOrNull
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.SerializedSelection
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.util.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainViewModel(
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

    fun launchLegacyActivity(context: Context): Boolean {
        return try {
            val intent = Intent()
            intent.setClassName(
                "app.revanced.manager.flutter",
                "app.revanced.manager.flutter.ExportSettingsActivity"
            )
            launcher.launch(intent)
            true
        } catch (e: Exception) {
            if (e !is ActivityNotFoundException) {
                context.toast(
                    context.getString(
                        R.string.legacy_import_failed
                    )
                )
            }
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

@Serializable
data class LegacySettings(
    val keystorePassword: String,
    val themeMode: Int? = null,
    val useDynamicTheme: Boolean? = null,
    val apiUrl: String? = null,
    val experimentalPatchesEnabled: Boolean? = null,
    val patchesAutoUpdate: Boolean? = null,
    val patchesChangeEnabled: Boolean? = null,
    val keystore: String? = null,
    val patches: SerializedSelection? = null,
)
