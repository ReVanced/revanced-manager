package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.requiredOptionsSet
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.toPatchSelection
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.patchCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

@OptIn(SavedStateHandleSaveableApi::class)
class SelectedAppInfoViewModel(
    input: SelectedAppInfo.ViewModelParams
) : ViewModel(), KoinComponent {
    private val bundleRepository: PatchBundleRepository = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val optionsRepository: PatchOptionsRepository = get()
    private val downloaderRepository: DownloaderRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val downloadedAppRepository: DownloadedAppRepository = get()
    private val rootInstaller: RootInstaller = get()
    private val pm: PM = get()
    private val savedStateHandle: SavedStateHandle = get()
    val prefs: PreferencesManager = get()

    val downloaders = downloaderRepository.loadedDownloadersFlow
    val packageName = input.packageName
    val localPath = input.localPath
    private val persistConfiguration = input.patches == null

    private val selectionFlow = MutableStateFlow(
        input.patches?.let(SelectionState::Customized) ?: SelectionState.Default
    )

    private val _selectedVersion = MutableStateFlow<SelectedVersion>(SelectedVersion.Auto)
    val selectedVersion: StateFlow<SelectedVersion> = _selectedVersion

    private val _selectedSource = MutableStateFlow<SelectedSource>(SelectedSource.Auto)
    val selectedSource: StateFlow<SelectedSource> = _selectedSource

    private val unscopedBundles = bundleRepository.scopedBundleInfoFlow(packageName, null)

    val versionPatchSelection = combine(selectionFlow, unscopedBundles) { selection, bundleInfo ->
        selection.patches(bundleInfo, allowIncompatible = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mostCompatibleVersions = versionPatchSelection.flatMapLatest { selection ->
        bundleRepository.suggestedVersions(packageName, selection)
    }

    val resolvedVersion = combine(
        _selectedVersion,
        mostCompatibleVersions,
    ) { selected, mostCompatible ->
        when (selected) {
            is SelectedVersion.Specific -> selected.version
            is SelectedVersion.Any -> null
            is SelectedVersion.Auto -> mostCompatible?.maxWithOrNull(
                compareBy<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key }
            )?.key
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val scopedBundles = resolvedVersion.flatMapLatest { version ->
        bundleRepository.scopedBundleInfoFlow(packageName, version)
    }

    val patchSelection = combine(selectionFlow, scopedBundles) { selection, bundleInfo ->
        selection.patches(bundleInfo, allowIncompatible = true)
    }

    val customSelection = combine(selectionFlow, scopedBundles) { selection, bundleInfo ->
        (selection as? SelectionState.Customized)?.patches(bundleInfo, allowIncompatible = true)
    }

    val incompatiblePatchCount = combine(selectionFlow, scopedBundles) { selection, bundleInfo ->
        val selectedPatches = selection.patches(bundleInfo, allowIncompatible = true)
        bundleInfo.sumOf { bundle ->
            bundle.incompatible.count { patch ->
                patch.name in selectedPatches[bundle.uid].orEmpty()
            }
        }
    }

    val resolvedSource = combine(
        _selectedSource,
        resolvedVersion,
    ) { source, version ->
        when {
            source == SelectedSource.Auto -> resolveAutoSource(version)
            isSourceValid(source, version) -> source
            else -> resolveAutoSource(version)
        }
    }

    var options: Options by savedStateHandle.saveable {
        viewModelScope.launch {
            if (!persistConfiguration) return@launch // TODO: save options for patched apps.
            val bundlePatches = unscopedBundles.first()
                .associate { it.uid to it.patches.associateBy { patch -> patch.name } }

            options = withContext(Dispatchers.Default) {
                optionsRepository.getOptions(packageName, bundlePatches)
            }
        }

        mutableStateOf(emptyMap())
    }
        private set

    val errorFlow = combine(downloaders, resolvedSource) { downloaderList, source ->
        when {
            source is SelectedSource.Downloader && matchingDownloaders(
                source,
                downloaderList
            ).isEmpty() -> Error.NoDownloaders

            else -> null
        }
    }

    var selectedAppInfo: PackageInfo? by mutableStateOf(null)
        private set

    fun updateVersion(version: SelectedVersion) {
        _selectedVersion.value = version
    }

    fun updateSource(source: SelectedSource) {
        _selectedSource.value = source
    }

    suspend fun reloadDownloaders() {
        downloaderRepository.reload()
    }

    fun getOptionsFiltered(bundles: List<PatchBundleInfo.Scoped>) = options.filtered(bundles)
    suspend fun hasSetRequiredOptions(patchSelection: PatchSelection): Boolean {
        val allowIncompatible = prefs.disablePatchVersionCompatCheck.get()
        val bundles = scopedBundles.first()

        return bundles.requiredOptionsSet(
            allowIncompatible = allowIncompatible,
            isSelected = { bundle, patch -> patch.name in patchSelection[bundle.uid].orEmpty() },
            optionsForPatch = { bundle, patch -> options[bundle.uid]?.get(patch.name) },
        )
    }

    suspend fun getPatcherParams(): Patcher.ViewModelParams {
        val allowIncompatible = prefs.disablePatchVersionCompatCheck.get()
        val bundles = scopedBundles.first()

        return Patcher.ViewModelParams(
            packageName,
            resolvedVersion.first(),
            resolvedSource.first(),
            selectionFlow.value.patches(bundles, allowIncompatible),
            getOptionsFiltered(bundles)
        )
    }

    fun updateConfiguration(
        selection: PatchSelection?,
        selectedOptions: Options
    ) = viewModelScope.launch {
        selectionFlow.value = selection?.let(SelectionState::Customized) ?: SelectionState.Default

        val filteredOptions = selectedOptions.filtered(scopedBundles.first())
        options = filteredOptions

        if (!persistConfiguration) return@launch

        selection?.let { selectionRepository.updateSelection(packageName, it) }
            ?: selectionRepository.resetSelectionForPackage(packageName)

        optionsRepository.saveOptions(packageName, filteredOptions)
    }

    init {
        viewModelScope.launch {
            reloadDownloaders()
            invalidateSelectedAppInfo()
        }

        localPath?.let { local ->
            viewModelScope.launch {
                val packageInfo = withContext(Dispatchers.IO) { pm.getPackageInfo(File(local)) }
                    ?: return@launch

                _selectedVersion.value = SelectedVersion.Specific(packageInfo.versionName!!)
                _selectedSource.value = SelectedSource.Local(local)
            }
        }

        viewModelScope.launch {
            if (prefs.disableSelectionWarning.get()) {
                val previous = selectionRepository.getSelection(packageName)
                if (previous.patchCount != 0) {
                    selectionFlow.value = SelectionState.Customized(previous)
                }
            }
        }

        viewModelScope.launch {
            combine(_selectedSource, resolvedVersion) { source, version -> source to version }
                .collect { (source, version) ->
                    if (source != SelectedSource.Auto && !isSourceValid(source, version)) {
                        _selectedSource.value = SelectedSource.Auto
                    }
                }
        }
    }

    enum class Error(@param:StringRes val resourceId: Int) {
        NoDownloaders(R.string.no_downloader_available)
    }

    private suspend fun invalidateSelectedAppInfo() {
        selectedAppInfo = withContext(Dispatchers.IO) {
            pm.getPackageInfo(packageName) ?: localPath?.let { pm.getPackageInfo(File(it)) }
        }
    }

    private suspend fun resolveAutoSource(version: String?): SelectedSource {
        val installedPackage = pm.getPackageInfo(packageName)
        val installedApp = installedAppRepository.get(packageName)
        val installType = installedApp?.installType
        val isInstalledVersionMatch = version == null || installedPackage?.versionName == version

        if (
            installedPackage != null &&
            isInstalledVersionMatch &&
            installType != InstallType.DEFAULT &&
            !(installType == InstallType.MOUNT && !rootInstaller.hasRootAccess()) &&
            installedPackage.applicationInfo?.splitSourceDirs.isNullOrEmpty()
        ) {
            return SelectedSource.Installed
        }

        if (version != null) {
            downloadedAppRepository.get(packageName, version)?.let { app ->
                return SelectedSource.Downloaded(
                    path = downloadedAppRepository.getApkFileForApp(app).path,
                    version = version
                )
            }
        }

        return SelectedSource.Downloader()
    }

    private suspend fun isSourceValid(source: SelectedSource, version: String?): Boolean =
        when (source) {
            SelectedSource.Auto -> true
            SelectedSource.Installed -> {
                val installedPackage = pm.getPackageInfo(packageName) ?: return false
                val installedApp = installedAppRepository.get(packageName)

                installedApp?.installType != InstallType.DEFAULT &&
                    !(installedApp?.installType == InstallType.MOUNT && !rootInstaller.hasRootAccess()) &&
                    (version == null || installedPackage.versionName == version)
            }

            is SelectedSource.Downloaded -> version == null || source.version == version
            is SelectedSource.Local -> {
                val packageInfo = withContext(Dispatchers.IO) { pm.getPackageInfo(File(source.path)) }
                    ?: return false
                version == null || packageInfo.versionName == version
            }

            is SelectedSource.Downloader -> true
        }

    private fun matchingDownloaders(
        source: SelectedSource.Downloader,
        downloaderList: List<app.revanced.manager.network.downloader.LoadedDownloader>
    ) = downloaderList.filter { downloader ->
        (source.packageName == null || downloader.packageName == source.packageName) &&
            (source.className == null || downloader.className == source.className)
    }

    private companion object {
        /**
         * Returns a copy with all nonexistent options removed.
         */
        private fun Options.filtered(bundles: List<PatchBundleInfo.Scoped>): Options =
            buildMap options@{
                bundles.forEach bundles@{ bundle ->
                    val bundleOptions = this@filtered[bundle.uid] ?: return@bundles

                    val patches = bundle.patches.associateBy { it.name }

                    this@options[bundle.uid] = buildMap bundleOptions@{
                        bundleOptions.forEach patch@{ (patchName, values) ->
                            // Get all valid option keys for the patch.
                            val validOptionKeys =
                                patches[patchName]?.options?.map { it.name }?.toSet() ?: return@patch

                            this@bundleOptions[patchName] = values.filterKeys { key ->
                                key in validOptionKeys
                            }
                        }
                    }
                }
            }
    }
}

private sealed interface SelectionState : Parcelable {
    fun patches(bundles: List<PatchBundleInfo.Scoped>, allowIncompatible: Boolean): PatchSelection

    @Parcelize
    data class Customized(val patchSelection: PatchSelection) : SelectionState {
        override fun patches(bundles: List<PatchBundleInfo.Scoped>, allowIncompatible: Boolean) =
            bundles.toPatchSelection(allowIncompatible) { uid, patch ->
                patchSelection[uid]?.contains(patch.name) ?: false
            }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<PatchBundleInfo.Scoped>, allowIncompatible: Boolean) =
            bundles.toPatchSelection(allowIncompatible) { _, patch -> patch.include }
    }
}
