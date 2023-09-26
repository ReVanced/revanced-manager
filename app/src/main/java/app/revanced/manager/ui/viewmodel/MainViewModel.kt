package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.asRemoteOrNull
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.SerializedSelection
import app.revanced.manager.ui.theme.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

class MainViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val patchSelectionRepository: PatchSelectionRepository,
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

    fun launchLegacyActivity() = viewModelScope.launch {
        try {
            val intent = Intent()
            intent.setClassName(
                "app.revanced.manager.flutter",
                "app.revanced.manager.flutter.ExportSettingsActivity"
            )
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyLegacySettings(data: String) = viewModelScope.launch {
        prefs.showAutoUpdatesDialog.update(false)
        val jsonData = JSONObject(data)
        val keysIterator = jsonData.keys()
        while (keysIterator.hasNext()) {
            try {
                val key = keysIterator.next()
                val value = jsonData.get(key)

                when (key) {
                    "themeMode" -> {
                        val themeMap = mapOf(
                            0 to Theme.SYSTEM,
                            1 to Theme.LIGHT,
                            2 to Theme.DARK
                        )
                        val theme = themeMap[value as Int]!!
                        prefs.theme.update(theme)
                    }
                    "useDynamicTheme" -> {
                        prefs.dynamicColor.update(value == 1)
                    }
                    "apiUrl" -> {
                        prefs.api.update(value as String)
                    }
                    "experimentalPatchesEnabled" -> {
                        prefs.allowExperimental.update(value == 1)
                    }
                    "patchesAutoUpdate" -> {
                        with(patchBundleRepository) {
                            sources
                                .first()
                                .find { it.uid == 0 }
                                ?.asRemoteOrNull
                                ?.setAutoUpdate(value == 1)

                            updateCheck()
                        }
                    }
                    "patchesChangeEnabled" -> {
                        // TODO: Implement setting
                    }
                    "showPatchesChangeWarning" -> {
                        // TODO: Implement setting
                    }
                    "keystore" -> {
                        prefs.keystoreCommonName.update("ReVanced")
                        prefs.keystorePass.update(jsonData.get("keystorePassword") as String)

                        val keystorePath = app.getDir("signing", Context.MODE_PRIVATE)
                            .resolve("manager.keystore").toPath()
                        val keystoreBytes = Base64.decode(value as String, Base64.DEFAULT)
                        keystorePath.toFile().writeBytes(keystoreBytes)
                    }
                    "savedPatches" -> {
                        val bundleUid = patchBundleRepository.sources.first().first().uid
                        val selection = Json.decodeFromString<SerializedSelection>(value as String)
                        patchSelectionRepository.import(bundleUid, selection)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}