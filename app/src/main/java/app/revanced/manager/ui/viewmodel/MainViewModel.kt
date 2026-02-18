package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File

class MainViewModel(
    private val patchBundleRepository: PatchBundleRepository,
    private val patchSelectionRepository: PatchSelectionRepository,
    private val downloadedAppRepository: DownloadedAppRepository,
    private val installedAppRepository: InstalledAppRepository,
    private val keystoreManager: KeystoreManager,
    private val app: Application,
    val prefs: PreferencesManager,
    private val json: Json
) : ViewModel() {
    private val appSelectChannel = Channel<SelectedApp>()
    val appSelectFlow = appSelectChannel.receiveAsFlow()
    private val legacyImportActivityChannel = Channel<Intent>()
    val legacyImportActivityFlow = legacyImportActivityChannel.receiveAsFlow()

    private suspend fun suggestedVersion(packageName: String) =
        patchBundleRepository.suggestedVersions.first()[packageName]

    private suspend fun findDownloadedApp(app: SelectedApp): SelectedApp.Local? {
        if (app !is SelectedApp.Search) return null

        val suggestedVersion = suggestedVersion(app.packageName) ?: return null

        val downloadedApp =
            downloadedAppRepository.get(app.packageName, suggestedVersion, markUsed = true)
                ?: return null
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

    init {
        viewModelScope.launch {
            if (!prefs.firstLaunch.get()) return@launch
            legacyImportActivityChannel.send(Intent().apply {
                setClassName(
                    "app.revanced.manager.flutter",
                    "app.revanced.manager.flutter.ExportSettingsActivity"
                )
            })
        }
    }

    fun applyLegacySettings(flutterPrefs: SharedPreferences) {
        Log.d(tag, "Migrating flutter preferences")
        val data = JSONObject().apply {
            put("keystorePassword", "s3cur3p@ssw0rd")

            val allEntries: Map<String, *> = flutterPrefs.all
            for ((key, value) in allEntries) {
                put(key.replace("flutter.", ""), value)
            }

            File(app.getExternalFilesDir(null), "/revanced-manager.keystore").apply {
                if (exists()) {
                    val keystoreBase64 = Base64.encodeToString(readBytes(), Base64.DEFAULT)
                    put("keystore", keystoreBase64)
                    delete()
                }
            }

            File(app.filesDir.parentFile.absolutePath, "/app_flutter/selected-patches.json").apply {
                if (exists())
                {
                    val patches = String(readBytes(), Charsets.UTF_8)
                    put("patches", JSONObject(patches))
                }
            }
        }

        flutterPrefs.edit(commit = true) { clear() }

        val settings = try {
            json.decodeFromString<LegacySettings>(data.toString())
        } catch (e: SerializationException) {
            app.toast(app.getString(R.string.legacy_import_failed))
            Log.e(tag, "Legacy settings data could not be deserialized", e)
            return
        }

        applyLegacySettings(settings)
    }

    private fun applyLegacySettings(settings: LegacySettings) = viewModelScope.launch {
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
        settings.usePrereleases?.let { prereleases ->
            prefs.useManagerPrereleases.update(prereleases)
            prefs.usePatchesPrereleases.update(prereleases)
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
                "alias",
                settings.keystorePassword,
                keystoreBytes.inputStream()
            )
        }
        settings.patches?.let { selection ->
            patchSelectionRepository.import(0, selection)
        }
        settings.patchedApps?.let { apps ->
            val LIST_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu!"
            json.decodeFromString<List<String>>(apps.removePrefix(LIST_PREFIX)).forEach { appJson ->
                val patchedApp = json.decodeFromString<LegacyPatchedApp>(appJson)
                installedAppRepository.addOrUpdate(
                    patchedApp.packageName,
                    patchedApp.packageName,
                    patchedApp.version,
                    if (patchedApp.isRooted) InstallType.MOUNT else InstallType.DEFAULT,
                    mapOf(0 to patchedApp.appliedPatches.toSet())
                )
            }
        }
        Log.d(tag, "Imported legacy settings")
    }

    @Serializable
    private data class LegacyPatchedApp(
        val packageName: String,
        val version: String,
        val isRooted: Boolean,
        val appliedPatches: List<String>,
    )

    @Serializable
    private data class LegacySettings(
        val keystorePassword: String,
        val themeMode: Int? = null,
        val useDynamicTheme: Boolean? = null,
        val usePrereleases: Boolean? = null,
        val apiUrl: String? = null,
        val experimentalPatchesEnabled: Boolean? = null,
        val patchesAutoUpdate: Boolean? = null,
        val patchesChangeEnabled: Boolean? = null,
        val keystore: String? = null,
        val patches: SerializedSelection? = null,
        val patchedApps: String? = null,
    )
}
