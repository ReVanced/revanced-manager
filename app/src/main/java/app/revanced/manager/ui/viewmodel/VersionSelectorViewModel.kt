package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.cachedIn
import androidx.paging.map
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.plugin.downloader.DownloaderPlugin
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.network.downloader.ParceledDownloaderApp
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.PM
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VersionSelectorViewModel(
    val packageName: String
) : ViewModel(), KoinComponent {
    private val downloadedAppRepository: DownloadedAppRepository by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val patchBundleRepository: PatchBundleRepository by inject()
    private val downloaderPluginRepository: DownloaderPluginRepository by inject()
    private val pm: PM by inject()
    val rootInstaller: RootInstaller by inject()

    var installedApp: Pair<PackageInfo, InstalledApp?>? by mutableStateOf(null)
        private set
    var requiredVersion: String? by mutableStateOf(null)
        private set
    var selectedVersion: SelectedApp? by mutableStateOf(null)
        private set

    private var nonSuggestedVersionDialogSubject by mutableStateOf<SelectedApp?>(null)
    val showNonSuggestedVersionDialog by derivedStateOf { nonSuggestedVersionDialogSubject != null }

    private var suggestedVersion: String? = null

    init {
        viewModelScope.launch {
            val packageInfo = async(Dispatchers.IO) { pm.getPackageInfo(packageName) }
            val installedAppDeferred =
                async(Dispatchers.IO) { installedAppRepository.get(packageName) }

            installedApp =
                packageInfo.await()?.let {
                    it to installedAppDeferred.await()
                }
        }

        viewModelScope.launch {
            suggestedVersion = patchBundleRepository.suggestedVersions.first()[packageName]
        }
    }

    val supportedVersions = patchBundleRepository.bundles.map supportedVersions@{ bundles ->
        var patchesWithoutVersions = 0

        bundles.flatMap { (_, bundle) ->
            bundle.patches.flatMap { patch ->
                patch.compatiblePackages.orEmpty()
                    .filter { it.packageName == packageName }
                    .onEach { if (it.versions == null) patchesWithoutVersions++ }
                    .flatMap { it.versions.orEmpty() }
            }
        }.groupingBy { it }
            .eachCount()
            .toMutableMap()
            .apply {
                replaceAll { _, count ->
                    count + patchesWithoutVersions
                }
            }
    }.flowOn(Dispatchers.Default)

    val hasInstalledPlugins = downloaderPluginRepository.pluginStates.map { it.isNotEmpty() }
    val downloadersFlow = downloaderPluginRepository.loadedPluginsFlow

    private var downloaderPlugin: LoadedDownloaderPlugin? by mutableStateOf(null)
    val downloadableApps by derivedStateOf {
        downloaderPlugin?.let { plugin ->
            Pager(
                config = plugin.pagingConfig
            ) {
                plugin.createPagingSource(
                    DownloaderPlugin.SearchParameters(
                        packageName,
                        suggestedVersion
                    )
                )
            }.flow.map { pagingData ->
                pagingData.map {
                    SelectedApp.Download(
                        it.packageName,
                        it.version,
                        ParceledDownloaderApp(plugin, it)
                    )
                }
            }
        }?.flowOn(Dispatchers.Default)?.cachedIn(viewModelScope)
    }

    val downloadedVersions = downloadedAppRepository.getAll().map { downloadedApps ->
        downloadedApps
            .filter { it.packageName == packageName }
            .map {
                SelectedApp.Local(
                    it.packageName,
                    it.version,
                    downloadedAppRepository.getApkFileForApp(it),
                    false
                )
            }
    }

    fun selectDownloaderPlugin(plugin: LoadedDownloaderPlugin) {
        downloaderPlugin = plugin
    }

    fun dismissNonSuggestedVersionDialog() {
        nonSuggestedVersionDialogSubject = null
    }

    fun select(app: SelectedApp) {
        if (requiredVersion != null && app.version != requiredVersion) {
            nonSuggestedVersionDialogSubject = app
            return
        }

        selectedVersion = app
    }
}