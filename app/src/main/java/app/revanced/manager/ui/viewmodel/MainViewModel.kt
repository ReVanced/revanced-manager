package app.revanced.manager.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.SerializedSelection
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainViewModel(
    private val patchBundleRepository: PatchBundleRepository,
    private val patchSelectionRepository: PatchSelectionRepository,
    private val downloadedAppRepository: DownloadedAppRepository,
    private val keystoreManager: KeystoreManager,
    private val app: Application,
    val prefs: PreferencesManager
) : ViewModel() {
    private val appSelectChannel = Channel<SelectedApp>()
    val appSelectFlow = appSelectChannel.receiveAsFlow()

    private suspend fun suggestedVersion(packageName: String) =
        patchBundleRepository.suggestedVersions.first()[packageName]

    private suspend fun findDownloadedApp(app: SelectedApp): SelectedApp.Local? {
        if (app !is SelectedApp.Search) return null

        val suggestedVersion = suggestedVersion(app.packageName) ?: return null

        val downloadedApp =
            downloadedAppRepository.get(app.packageName, suggestedVersion, markUsed = true) ?: return null
        return SelectedApp.Local(
            downloadedApp.packageName,
            downloadedApp.version,
            downloadedAppRepository.getApkFileForApp(downloadedApp),
            false
        )
    }

    fun selectApp(app: SelectedApp) = viewModelScope.launch {
        appSelectChannel.send(findDownloadedApp(app) ?: app)
    }

    fun selectApp(packageName: String) = viewModelScope.launch {
        selectApp(SelectedApp.Search(packageName, suggestedVersion(packageName)))
    }

    fun importLegacySettings(componentActivity: ComponentActivity) {
        if (!prefs.firstLaunch.getBlocking()) return

        try {
            val launcher = componentActivity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getStringExtra("data")?.let {
                        applyLegacySettings(it)
                    } ?: app.toast(app.getString(R.string.legacy_import_failed))
                } else {
                    app.toast(app.getString(R.string.legacy_import_failed))
                }
            }

            val intent = Intent().apply {
                setClassName(
                    "app.revanced.manager.flutter",
                    "app.revanced.manager.flutter.ExportSettingsActivity"
                )
            }

            launcher.launch(intent)
        } catch (e: Exception) {
            if (e !is ActivityNotFoundException) {
                app.toast(app.getString(R.string.legacy_import_failed))
                Log.e(tag, "Failed to launch legacy import activity: $e")
            }
        }
    }

    private fun applyLegacySettings(data: String) = viewModelScope.launch {
        val json = Json { ignoreUnknownKeys = true }
        val settings = json.decodeFromString<LegacySettings>(data)

        settings.themeMode?.let { theme ->
            val themeMap = mapOf(
                0 to Theme.SYSTEM,
                1 to Theme.LIGHT,
                2 to Theme.DARK
            )
            prefs.theme.update(themeMap[theme] ?: Theme.SYSTEM)
        }
        settings.useDynamicTheme?.let { dynamicColor ->
            prefs.dynamicColor.update(dynamicColor)
        }
        settings.apiUrl?.let { api ->
            prefs.api.update(api.removeSuffix("/"))
        }
        settings.experimentalPatchesEnabled?.let { allowExperimental ->
            prefs.disablePatchVersionCompatCheck.update(allowExperimental)
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
    }

    @Serializable
    private data class LegacySettings(
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
}
