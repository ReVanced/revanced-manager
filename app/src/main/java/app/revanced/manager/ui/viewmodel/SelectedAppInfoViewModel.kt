package app.revanced.manager.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Parcelable
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
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
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.plugin.downloader.GetScope
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.plugin.downloader.UserInteractionException
import app.revanced.manager.ui.model.BundleInfo
import app.revanced.manager.ui.model.BundleInfo.Extensions.bundleInfoFlow
import app.revanced.manager.ui.model.BundleInfo.Extensions.requiredOptionsSet
import app.revanced.manager.ui.model.BundleInfo.Extensions.toPatchSelection
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.ui.model.navigation.SelectedApplicationInfo
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(SavedStateHandleSaveableApi::class, PluginHostApi::class)
class SelectedAppInfoViewModel(
    input: SelectedApplicationInfo.ViewModelParams
) : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val bundleRepository: PatchBundleRepository = get()
    private val selectionRepository: PatchSelectionRepository = get()
    private val optionsRepository: PatchOptionsRepository = get()
    private val pluginsRepository: DownloaderPluginRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val rootInstaller: RootInstaller = get()
    private val pm: PM = get()
    private val savedStateHandle: SavedStateHandle = get()
    val prefs: PreferencesManager = get()
    val plugins = pluginsRepository.loadedPluginsFlow
    val desiredVersion = input.app.version
    val packageName = input.app.packageName

    private val persistConfiguration = input.patches == null

    val hasRoot = rootInstaller.hasRootAccess()
    var installedAppData: Pair<SelectedApp.Installed, InstalledApp?>? by mutableStateOf(null)
        private set

    private var _selectedApp by savedStateHandle.saveable {
        mutableStateOf(input.app)
    }

    var selectedAppInfo: PackageInfo? by mutableStateOf(null)
        private set

    var selectedApp
        get() = _selectedApp
        set(value) {
            _selectedApp = value
            invalidateSelectedAppInfo()
        }

    init {
        invalidateSelectedAppInfo()
        viewModelScope.launch(Dispatchers.Main) {
            val packageInfo = async(Dispatchers.IO) { pm.getPackageInfo(packageName) }
            val installedAppDeferred =
                async(Dispatchers.IO) { installedAppRepository.get(packageName) }

            installedAppData =
                packageInfo.await()?.let {
                    SelectedApp.Installed(
                        packageName,
                        it.versionName!!
                    ) to installedAppDeferred.await()
                }
        }
    }

    val requiredVersion = combine(
        prefs.suggestedVersionSafeguard.flow,
        bundleRepository.suggestedVersions
    ) { suggestedVersionSafeguard, suggestedVersions ->
        if (!suggestedVersionSafeguard) return@combine null

        suggestedVersions[input.app.packageName]
    }

    var options: Options by savedStateHandle.saveable {
        val state = mutableStateOf<Options>(emptyMap())

        viewModelScope.launch {
            if (!persistConfiguration) return@launch // TODO: save options for patched apps.

            state.value = withContext(Dispatchers.Default) {
                val bundlePatches = bundleRepository.bundles.first()
                    .mapValues { (_, bundle) -> bundle.patches.associateBy { it.name } }

                optionsRepository.getOptions(packageName, bundlePatches)
            }
        }

        state
    }
        private set

    private var selectionState by savedStateHandle.saveable {
        if (input.patches != null)
            return@saveable mutableStateOf(SelectionState.Customized(input.patches))

        val selection: MutableState<SelectionState> = mutableStateOf(SelectionState.Default)

        // Try to get the previous selection if customization is enabled.
        viewModelScope.launch {
            if (!prefs.disableSelectionWarning.get()) return@launch

            val previous = selectionRepository.getSelection(packageName)
            if (previous.values.sumOf { it.size } == 0) return@launch
            selection.value = SelectionState.Customized(previous)
        }

        selection
    }

    var showSourceSelector by mutableStateOf(false)
        private set
    private var pluginAction: Pair<LoadedDownloaderPlugin, Job>? by mutableStateOf(null)
    val activePluginAction get() = pluginAction?.first?.packageName
    private var launchedActivity by mutableStateOf<CompletableDeferred<ActivityResult>?>(null)
    private val launchActivityChannel = Channel<Intent>()
    val launchActivityFlow = launchActivityChannel.receiveAsFlow()

    val errorFlow = combine(plugins, snapshotFlow { selectedApp }) { pluginsList, app ->
        when {
            app is SelectedApp.Search && pluginsList.isEmpty() -> Error.NoPlugins
            else -> null
        }
    }

    val bundleInfoFlow by derivedStateOf {
        bundleRepository.bundleInfoFlow(packageName, selectedApp.version)
    }

    fun showSourceSelector() {
        dismissSourceSelector()
        showSourceSelector = true
    }

    private fun cancelPluginAction() {
        pluginAction?.second?.cancel()
        pluginAction = null
    }

    fun dismissSourceSelector() {
        cancelPluginAction()
        showSourceSelector = false
    }

    fun searchUsingPlugin(plugin: LoadedDownloaderPlugin) {
        cancelPluginAction()
        pluginAction = plugin to viewModelScope.launch {
            try {
                val scope = object : GetScope {
                    override val hostPackageName = app.packageName
                    override val pluginPackageName = plugin.packageName
                    override suspend fun requestStartActivity(intent: Intent) =
                        withContext(Dispatchers.Main) {
                            if (launchedActivity != null) error("Previous activity has not finished")
                            try {
                                val result = with(CompletableDeferred<ActivityResult>()) {
                                    launchedActivity = this
                                    launchActivityChannel.send(intent)
                                    await()
                                }
                                when (result.resultCode) {
                                    Activity.RESULT_OK -> result.data
                                    Activity.RESULT_CANCELED -> throw UserInteractionException.Activity.Cancelled()
                                    else -> throw UserInteractionException.Activity.NotCompleted(
                                        result.resultCode,
                                        result.data
                                    )
                                }
                            } finally {
                                launchedActivity = null
                            }
                        }
                }

                withContext(Dispatchers.IO) {
                    plugin.get(scope, packageName, desiredVersion)
                }?.let { (data, version) ->
                    if (desiredVersion != null && version != desiredVersion) {
                        app.toast(app.getString(R.string.downloader_invalid_version))
                        return@launch
                    }
                    selectedApp = SelectedApp.Download(
                        packageName,
                        version,
                        ParceledDownloaderData(plugin, data)
                    )
                } ?: app.toast(app.getString(R.string.downloader_app_not_found))
            } catch (e: UserInteractionException.Activity) {
                app.toast(e.message!!)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.toast(app.getString(R.string.downloader_error, e.simpleMessage()))
                Log.e(tag, "Downloader.get threw an exception", e)
            } finally {
                pluginAction = null
                dismissSourceSelector()
            }
        }
    }

    fun handlePluginActivityResult(result: ActivityResult) {
        launchedActivity?.complete(result)
    }

    private fun invalidateSelectedAppInfo() = viewModelScope.launch {
        val info = when (val app = selectedApp) {
            is SelectedApp.Local -> withContext(Dispatchers.IO) { pm.getPackageInfo(app.file) }
            is SelectedApp.Installed -> withContext(Dispatchers.IO) { pm.getPackageInfo(app.packageName) }
            else -> null
        }

        selectedAppInfo = info
    }

    suspend fun hasSetRequiredOptions(patchSelection: PatchSelection) = bundleInfoFlow
        .first()
        .requiredOptionsSet(
            isSelected = { bundle, patch -> patch.name in patchSelection[bundle.uid]!! },
            optionsForPatch = { bundle, patch -> options[bundle.uid]?.get(patch.name) },
        )

    suspend fun getPatcherParams(): Patcher.ViewModelParams {
        val allowIncompatible = prefs.disablePatchVersionCompatCheck.get()
        val bundles = bundleInfoFlow.first()
        return Patcher.ViewModelParams(
            selectedApp,
            getPatches(bundles, allowIncompatible),
            getOptionsFiltered(bundles)
        )
    }

    fun getOptionsFiltered(bundles: List<BundleInfo>) = options.filtered(bundles)

    fun getPatches(bundles: List<BundleInfo>, allowIncompatible: Boolean) =
        selectionState.patches(bundles, allowIncompatible)

    fun getCustomPatches(
        bundles: List<BundleInfo>,
        allowIncompatible: Boolean
    ): PatchSelection? =
        (selectionState as? SelectionState.Customized)?.patches(bundles, allowIncompatible)

    fun updateConfiguration(selection: PatchSelection?, options: Options) = viewModelScope.launch {
        val bundles = bundleInfoFlow.first()

        selectionState = selection?.let(SelectionState::Customized) ?: SelectionState.Default

        val filteredOptions = options.filtered(bundles)
        this@SelectedAppInfoViewModel.options = filteredOptions

        if (!persistConfiguration) return@launch
        viewModelScope.launch(Dispatchers.Default) {
            selection?.let { selectionRepository.updateSelection(packageName, it) }
                ?: selectionRepository.clearSelection(packageName)

            optionsRepository.saveOptions(packageName, filteredOptions)
        }
    }

    enum class Error(@StringRes val resourceId: Int) {
        NoPlugins(R.string.downloader_no_plugins_available)
    }

    private companion object {
        /**
         * Returns a copy with all nonexistent options removed.
         */
        private fun Options.filtered(bundles: List<BundleInfo>): Options = buildMap options@{
            bundles.forEach bundles@{ bundle ->
                val bundleOptions = this@filtered[bundle.uid] ?: return@bundles

                val patches = bundle.all.associateBy { it.name }

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
    fun patches(bundles: List<BundleInfo>, allowIncompatible: Boolean): PatchSelection

    @Parcelize
    data class Customized(val patchSelection: PatchSelection) : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowIncompatible: Boolean) =
            bundles.toPatchSelection(
                allowIncompatible
            ) { uid, patch ->
                patchSelection[uid]?.contains(patch.name) ?: false
            }
    }

    @Parcelize
    data object Default : SelectionState {
        override fun patches(bundles: List<BundleInfo>, allowIncompatible: Boolean) =
            bundles.toPatchSelection(allowIncompatible) { _, patch -> patch.include }
    }
}

