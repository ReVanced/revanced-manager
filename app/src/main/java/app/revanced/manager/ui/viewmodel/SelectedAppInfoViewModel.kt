package app.revanced.manager.ui.viewmodel

import android.content.pm.PackageInfo
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.requiredOptionsSet
import app.revanced.manager.patcher.patch.PatchBundleInfo.Extensions.toPatchSelection
import app.revanced.manager.plugin.downloader.PluginHostApi
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(SavedStateHandleSaveableApi::class, PluginHostApi::class)
class SelectedAppInfoViewModel(
    private val input: SelectedAppInfo.ViewModelParams
) : ViewModel(), KoinComponent {
    private val bundleRepository: PatchBundleRepository = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val optionsRepository: PatchOptionsRepository = get()
    private val pluginsRepository: DownloaderPluginRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val downloadedAppRepository: DownloadedAppRepository = get()
    private val pm: PM = get()
    private val savedStateHandle: SavedStateHandle = get()
    private val prefs: PreferencesManager = get()
    val plugins = pluginsRepository.loadedPluginsFlow
    val packageName = input.packageName
    private val persistConfiguration = input.patches == null


    // User selection
    private var selectionFlow = MutableStateFlow(
        input.patches?.let { selection ->
            SelectionState.Customized(selection)
        } ?: SelectionState.Default
    )

    private val _selectedVersion = MutableStateFlow<SelectedVersion>(SelectedVersion.Auto)
    val selectedVersion: StateFlow<SelectedVersion> = _selectedVersion

    private val _selectedSource = MutableStateFlow<SelectedSource>(SelectedSource.Auto)
    val selectedSource: StateFlow<SelectedSource> = _selectedSource

    fun updateVersion(version: SelectedVersion) {
        _selectedVersion.value = version
    }
    fun updateSource(source: SelectedSource) {
        _selectedSource.value = source
    }
    fun updateConfiguration(
        selection: PatchSelection?,
        selectedOptions: Options
    ) = viewModelScope.launch {
        selectionFlow.value = selection?.let(SelectionState::Customized) ?: SelectionState.Default

        val filteredOptions = selectedOptions.filtered(bundleInfoFlow.first())
        options = filteredOptions

        if (persistConfiguration) {
            selection?.let { selectionRepository.updateSelection(packageName, it) }
                ?: selectionRepository.resetSelectionForPackage(packageName)

            optionsRepository.saveOptions(packageName, filteredOptions)
        }
    }


    // All patches for package
    val bundles = bundleRepository.scopedBundleInfoFlow(packageName, null)

    // Selection derived from selectionFlow
    val patchSelection = combine(
        selectionFlow,
        bundles,
    ) { selection, bundles ->
        selection.patches(bundles, allowIncompatible = true)
    }

    val customSelection = combine(
        selectionFlow,
        bundles,
    ) { selection, bundles ->
        (selection as? SelectionState.Customized)?.patches(bundles, allowIncompatible = true)
    }

    // Most compatible versions based on patch selection
    @OptIn(ExperimentalCoroutinesApi::class)
    val mostCompatibleVersions = patchSelection.flatMapLatest { patchSelection ->
        bundleRepository.suggestedVersions(
            packageName,
            patchSelection
        )
    }

    // Resolve actual version from user selection
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

    val incompatiblePatchCount = scopedBundles.map { bundles ->
        bundles.sumOf { bundle ->
            bundle.incompatible.size
        }
    }

    // Resolve actual source from user selection
    val resolvedSource = combine(
        _selectedSource,
        resolvedVersion
    ) { source, version ->
        when (source) {
            is SelectedSource.Installed -> source
            is SelectedSource.Local -> source
            is SelectedSource.Downloaded -> source
            is SelectedSource.Plugin -> source
            is SelectedSource.Auto -> {
                val app = version?.let {
                    downloadedAppRepository.get(packageName, it)
                }
                val file = app?.let {
                    downloadedAppRepository.getApkFileForApp(it)
                }

                file?.let { SelectedSource.Downloaded(it.path, version) }
                    ?: SelectedSource.Plugin(null)
            }
        }
    }

    val bundleInfoFlow by derivedStateOf {
        bundleRepository.scopedBundleInfoFlow(packageName, null)
    }

    var options: Options by savedStateHandle.saveable {
        viewModelScope.launch {
            if (!persistConfiguration) return@launch // TODO: save options for patched apps.
            val bundlePatches = bundleInfoFlow.first()
                .associate { it.uid to it.patches.associateBy { patch -> patch.name } }

            options = withContext(Dispatchers.Default) {
                optionsRepository.getOptions(packageName, bundlePatches)
            }
        }

        mutableStateOf(emptyMap())
    }
        private set


    val errorFlow = combine(
        plugins,
        resolvedSource,
    ) { pluginsList, source ->
        when {
            source is SelectedSource.Plugin && pluginsList.isEmpty() -> Error.NoPlugins
            else -> null
        }
    }



//    var installedAppData: Pair<SelectedApp.Installed, InstalledApp?>? by mutableStateOf(null)
//        private set

    private var _selectedApp by savedStateHandle.saveable {
        mutableStateOf(null)
    }

    var selectedAppInfo: PackageInfo? by mutableStateOf(null)
        private set

    var selectedApp
        get() = _selectedApp
        set(value) {
            _selectedApp = value
            invalidateSelectedAppInfo()
        }




    // TODO: Load from local file or downloaded app
    private fun invalidateSelectedAppInfo() = viewModelScope.launch {
        selectedAppInfo = pm.getPackageInfo(packageName)
    }

    fun getOptionsFiltered(bundles: List<PatchBundleInfo.Scoped>) = options.filtered(bundles)
    suspend fun hasSetRequiredOptions(patchSelection: PatchSelection) = bundleInfoFlow
        .first()
        .requiredOptionsSet(
            allowIncompatible = prefs.disablePatchVersionCompatCheck.get(),
            isSelected = { bundle, patch -> patch.name in patchSelection[bundle.uid]!! },
            optionsForPatch = { bundle, patch -> options[bundle.uid]?.get(patch.name) },
        )

    suspend fun getPatcherParams(): Patcher.ViewModelParams {
        val allowIncompatible = prefs.disablePatchVersionCompatCheck.get()
        val bundles = bundleInfoFlow.first()
        return Patcher.ViewModelParams(
            input.packageName,
            resolvedVersion.first(),
            resolvedSource.first(),
            patchSelection.first(),
            getOptionsFiltered(bundles)
        )
    }

    init {
        invalidateSelectedAppInfo()

        // Get the previous selection if customization is enabled.
        viewModelScope.launch {
            if (prefs.disableSelectionWarning.get()) {
                val previous = selectionRepository.getSelection(packageName)
                if (previous.patchCount == 0) return@launch
                selectionFlow.value = SelectionState.Customized(previous)
            }
        }

        // Get installed app info
        viewModelScope.launch {
            val packageInfo = async(Dispatchers.IO) { pm.getPackageInfo(packageName) }
            val installedAppDeferred =
                async(Dispatchers.IO) { installedAppRepository.get(packageName) }

//            installedAppData =
//                packageInfo.await()?.let {
//                    SelectedApp.Installed(
//                        packageName,
//                        it.versionName!!
//                    ) to installedAppDeferred.await()
//                }
        }
    }

    enum class Error(@param:StringRes val resourceId: Int) {
        NoPlugins(R.string.downloader_no_plugins_available)
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
                                patches[patchName]?.options?.map { it.key }?.toSet() ?: return@patch

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
            bundles.toPatchSelection(
                allowIncompatible
            ) { uid, patch ->
                patchSelection[uid]?.contains(patch.name) ?: false
            }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<PatchBundleInfo.Scoped>, allowIncompatible: Boolean) =
            bundles.toPatchSelection(allowIncompatible) { _, patch -> patch.include }
    }
}

