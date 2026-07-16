package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.NotificationCardType
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.enabled
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAppInfoScreen(
    onPatchSelectorClick: (SelectedApp, PatchSelection?, Options) -> Unit,
    onRequiredOptions: (SelectedApp, PatchSelection?, Options) -> Unit,
    onPatchClick: () -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val resources = LocalResources.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by vm.bundleInfoFlow.collectAsStateWithLifecycle(emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val effectiveAllowIncompatible = allowIncompatiblePatches || vm.selectedApp.version == null

    val patches by remember(bundles, effectiveAllowIncompatible) {
        derivedStateOf {
            vm.getPatches(bundles, effectiveAllowIncompatible)
        }
    }
    val versionOptions by remember(bundles, patches, packageName, allowIncompatiblePatches) {
        derivedStateOf {
            buildVersionOptions(
                bundles = bundles,
                selectedPatches = patches,
                packageName = packageName,
                allowIncompatible = allowIncompatiblePatches
            )
        }
    }
    val strictVersionOptions by remember(bundles, patches, packageName) {
        derivedStateOf {
            buildVersionOptions(
                bundles = bundles,
                selectedPatches = patches,
                packageName = packageName,
                allowIncompatible = false
            )
        }
    }
    val selectedVersionLabel by remember(vm.selectedApp.version) {
        derivedStateOf {
            vm.selectedApp.version ?: resources.getString(R.string.selected_app_meta_any_version)
        }
    }
    var showVersionSelector by remember { mutableStateOf(false) }
    val selectedPatchCount = patches.values.sumOf { it.size }
    val hasModifiedPatchSelection by remember(bundles, effectiveAllowIncompatible) {
        derivedStateOf {
            vm.hasModifiedPatchSelection(bundles, effectiveAllowIncompatible)
        }
    }
    val showVersionCompatibilityWarning by remember(
        vm.selectedApp.version,
        allowIncompatiblePatches,
        strictVersionOptions
    ) {
        derivedStateOf {
            val selectedVersion = vm.selectedApp.version ?: return@derivedStateOf false
            allowIncompatiblePatches &&
                    strictVersionOptions.versions.isNotEmpty() &&
                    selectedVersion !in strictVersionOptions.versions
        }
    }

    LaunchedEffect(versionOptions, vm.selectedApp.version) {
        if (versionOptions.unrestricted) return@LaunchedEffect

        val selectedVersion = vm.selectedApp.version
        if (selectedVersion != null && selectedVersion in versionOptions.versions) {
            return@LaunchedEffect
        }

        vm.setTargetVersion(versionOptions.versions.firstOrNull())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handleDownloaderActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        launcher.launch(intent)
    }
    val composableScope = rememberCoroutineScope()

    val sourcePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let(vm::handleStorageResult) }
    )
    EventEffect(flow = vm.storageSelectionFlow) { app ->
        vm.selectedApp = app
        vm.dismissSourceSelector()
    }

    val error by vm.errorFlow.collectAsStateWithLifecycle(null)
    val downloaders by vm.downloaders.collectAsStateWithLifecycle(emptyMap())

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_info),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            // Hide the FAB when no patches are selected.
            if (selectedPatchCount == 0) return@Scaffold

            // Only hide the FAB for errors that genuinely block patching.
            // No-downloader errors are NOT blocking because the storage picker is the fallback.
            val blockingError = error?.takeIf {
                it != SelectedAppInfoViewModel.Error.NoDownloadersInstalled
            }
            if (blockingError != null) return@Scaffold

            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        stringResource(R.string.patch)
                    )
                },
                onClick = patchClick@{
                    // If the selected source is Auto (Search) but nothing can be resolved
                    // (no installed app, no downloaded APK, no downloader), prompt the user
                    // to pick an APK from storage instead of failing silently.
                    if (vm.selectedApp is SelectedApp.Search &&
                        vm.resolveAutoSource(vm.selectedApp.version) is SelectedApp.Search &&
                        downloaders.isEmpty()
                    ) {
                        sourcePickerLauncher.launch(APK_MIMETYPE)
                        return@patchClick
                    }

                    composableScope.launch {
                        if (!vm.hasSetRequiredOptions(patches, effectiveAllowIncompatible)) {
                            onRequiredOptions(
                                vm.selectedApp,
                                vm.getCustomPatches(bundles, effectiveAllowIncompatible),
                                vm.options
                            )
                            return@launch
                        }

                        onPatchClick()
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->

        if (showVersionSelector) {
            VersionSelectorDialog(
                selectedVersion = vm.selectedApp.version,
                availableVersions = versionOptions.versions,
                allowAnyVersion = versionOptions.unrestricted,
                onDismissRequest = { showVersionSelector = false },
                onSelect = { version ->
                    vm.setTargetVersion(version)
                    showVersionSelector = false
                }
            )
        }

        if (vm.showSourceSelector) {
            val selectedVersion = vm.selectedApp.version
            val autoSelection = vm.resolveAutoSource(selectedVersion)

            AppSourceSelectorDialog(
                downloaders = downloaders,
                downloadedApps = vm.downloadedApps,
                activeSearchJob = vm.activeDownloader,
                requiredVersion = selectedVersion,
                autoSelection = autoSelection,
                onDismissRequest = vm::dismissSourceSelector,
                onSelectAuto = {
                    vm.selectedApp = autoSelection
                    vm.dismissSourceSelector()
                },
                onSelectDownloader = vm::searchUsingDownloader,
                onSelectFromStorage = { sourcePickerLauncher.launch(APK_MIMETYPE) },
                onSelect = {
                    vm.selectedApp = it
                    vm.dismissSourceSelector()
                }
            )
        }

        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppInfo(vm.selectedAppInfo, placeholderLabel = packageName) {
                Text(
                    version ?: stringResource(R.string.selected_app_meta_any_version),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PageItem(
                R.string.patch_selector_item,
                stringResource(
                    R.string.patch_selector_item_description,
                    selectedPatchCount
                ),
                warningDescription = if (hasModifiedPatchSelection) {
                    stringResource(R.string.patch_selection_changed_warning)
                } else {
                    null
                },
                onClick = {
                    onPatchSelectorClick(
                        vm.selectedApp,
                        vm.getCustomPatches(
                            bundles,
                            effectiveAllowIncompatible
                        ),
                        vm.options
                    )
                }
            )
            if (versionOptions.versions.isNotEmpty()) {
                PageItem(
                    R.string.version,
                    selectedVersionLabel,
                    warningDescription = if (showVersionCompatibilityWarning) {
                        stringResource(R.string.version_compatibility_warning)
                    } else {
                        null
                    },
                    onClick = { showVersionSelector = true }
                )
            }
            val autoSourceSubtitle = run {
                val resolved = vm.resolveAutoSource(vm.selectedApp.version)
                when {
                    resolved is SelectedApp.Installed -> stringResource(R.string.apk_source_auto_installed)
                    resolved is SelectedApp.Local -> stringResource(R.string.apk_source_auto_downloaded)
                    downloaders.isNotEmpty() -> stringResource(R.string.apk_source_auto_downloader)
                    else -> stringResource(R.string.apk_source_auto_storage)
                }
            }
            PageItem(
                R.string.apk_source_selector_item,
                when (val app = vm.selectedApp) {
                    is SelectedApp.Search -> autoSourceSubtitle
                    is SelectedApp.Installed -> stringResource(R.string.apk_source_installed)
                    is SelectedApp.Download -> stringResource(
                        R.string.apk_source_downloader,
                        downloaders.values
                            .flatten()
                            .find { it.className == app.data.downloaderClassName }
                            ?.packageLabel ?: app.data.downloaderPackageName
                    )

                    is SelectedApp.Local -> stringResource(R.string.apk_source_local)
                },
                onClick = {
                    vm.showSourceSelector()
                }
            )
            // Only show inline error text for truly blocking errors, not no-downloader
            // errors which are handled gracefully via the storage picker fallback.
            val inlineError = error?.takeIf {
                it != SelectedAppInfoViewModel.Error.NoDownloadersInstalled
            }
            inlineError?.let {
                Text(
                    text = stringResource(it.resourceId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .padding(horizontal = 24.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val needsInternet =
                    vm.selectedApp.let { it is SelectedApp.Search || it is SelectedApp.Download }

                if (needsInternet && networkMetered) NotificationCard(
                    type = NotificationCardType.WARNING,
                    icon = Icons.Outlined.WarningAmber,
                    text = stringResource(R.string.network_metered_warning),
                    onDismiss = null
                )
            }
        }
    }
}

@Composable
private fun PageItem(
    @StringRes title: Int,
    description: String,
    warningDescription: String? = null,
    warningColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(start = 8.dp),
        headlineContent = {
            Text(
                stringResource(title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    description,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium
                )

                warningDescription?.let {
                    Text(
                        text = "(!) $it",
                        color = if (warningColor == Color.Unspecified) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            warningColor
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    )
}

private data class VersionOptions(
    val versions: List<String>,
    val unrestricted: Boolean
)

private fun buildVersionOptions(
    bundles: List<PatchBundleInfo.Scoped>,
    selectedPatches: PatchSelection,
    packageName: String,
    allowIncompatible: Boolean
): VersionOptions {
    val selected = bundles.flatMap { bundle ->
        val selectedNames = selectedPatches[bundle.uid].orEmpty()
        bundle.patches.filter { it.name in selectedNames }
    }

    val constraints = selected.mapNotNull { patch ->
        patch.versionConstraintFor(packageName)
    }

    if (constraints.isEmpty() || allowIncompatible) {
        val knownVersions = bundles.asSequence()
            .flatMap { bundle -> bundle.patches.asSequence() }
            .mapNotNull { patch -> patch.versionConstraintFor(packageName) }
            .flatMap { versions -> versions.asSequence() }
            .distinct()
            .sortedDescending()
            .toList()

        return VersionOptions(versions = knownVersions, unrestricted = true)
    }

    val intersection = constraints
        .map { it.toSet() }
        .reduce { acc, versions -> acc intersect versions }
        .toList()
        .sortedDescending()

    return VersionOptions(versions = intersection, unrestricted = false)
}

private fun PatchInfo.versionConstraintFor(packageName: String): Set<String>? {
    val pkg = compatiblePackages?.firstOrNull { it.packageName == packageName } ?: return null
    return pkg.versions?.toSet()?.takeIf { it.isNotEmpty() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun VersionSelectorDialog(
    selectedVersion: String?,
    availableVersions: List<String>,
    allowAnyVersion: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (String?) -> Unit
) {
    FullscreenDialog(onDismissRequest) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.version),
                    onBackClick = onDismissRequest
                )
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                if (allowAnyVersion) {
                    item(key = "any") {
                        ListItem(
                            modifier = Modifier.clickable { onSelect(null) },
                            headlineContent = { Text(stringResource(R.string.selected_app_meta_any_version)) },
                            supportingContent = if (selectedVersion == null) {
                                { Text(stringResource(R.string.this_version)) }
                            } else {
                                null
                            },
                            colors = transparentListItemColors
                        )
                    }
                }

                items(
                    items = availableVersions,
                    key = { version -> "version_$version" }
                ) { version ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(version) },
                        headlineContent = { Text(version) },
                        supportingContent = if (selectedVersion == version) {
                            { Text(stringResource(R.string.this_version)) }
                        } else {
                            null
                        },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppSourceSelectorDialog(
    downloaders: Map<String, List<LoadedDownloader>>,
    downloadedApps: List<SelectedApp.Local>,
    activeSearchJob: LoadedDownloader?,
    requiredVersion: String?,
    autoSelection: SelectedApp,
    onDismissRequest: () -> Unit,
    onSelectAuto: () -> Unit,
    onSelectDownloader: (LoadedDownloader) -> Unit,
    onSelectFromStorage: () -> Unit,
    onSelect: (SelectedApp) -> Unit,
) {
    val canSelect = activeSearchJob == null

    val hasDownloaded = downloadedApps.any {
        requiredVersion == null || it.version == requiredVersion
    }
    val hasAutoSource =
        downloaders.isNotEmpty() ||
                hasDownloaded ||
                autoSelection is SelectedApp.Installed

    FullscreenDialog(onDismissRequest) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.app_source_dialog_title),
                    onBackClick = onDismissRequest
                )
            }
        ) { paddingValues ->
            LazyColumn(Modifier.padding(paddingValues)) {
                item("auto") {
                    ListItem(
                        modifier = Modifier
                            .clickable(
                                enabled = canSelect && hasAutoSource,
                                onClick = onSelectAuto
                            )
                            .enabled(hasAutoSource),
                        headlineContent = {
                            Text(stringResource(R.string.app_source_dialog_option_auto))
                        },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (hasAutoSource)
                                        R.string.app_source_dialog_option_auto_description
                                    else
                                        R.string.app_source_dialog_option_auto_unavailable
                                )
                            )
                        },
                        colors = transparentListItemColors
                    )
                }

                item("storage") {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onSelectFromStorage),
                        headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                        supportingContent = { Text(stringResource(R.string.select_from_storage_description)) },
                        colors = transparentListItemColors
                    )
                }

                if (downloadedApps.isNotEmpty()) {
                    item { HorizontalDivider() }

                    items(downloadedApps, key = { it.version }) { app ->
                        val usable =
                            requiredVersion == null || app.version == requiredVersion

                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = canSelect && usable) { onSelect(app) }
                                .enabled(usable),
                            headlineContent = { Text(app.packageName) },
                            supportingContent = { Text(app.version) },
                            overlineContent = {
                                Text(stringResource(R.string.source_selector_category_downloaded))
                            },
                            colors = transparentListItemColors
                        )
                    }
                }

                if (downloaders.isNotEmpty()) {
                    item { HorizontalDivider() }

                    downloaders.forEach { (name, list) ->
                        items(list) { downloader ->
                            ListItem(
                                modifier = Modifier.clickable(enabled = canSelect) {
                                    onSelectDownloader(downloader)
                                },
                                headlineContent = { Text(downloader.name) },
                                trailingContent = {
                                    if (activeSearchJob == downloader) {
                                        LoadingIndicator()
                                    }
                                },
                                overlineContent = {
                                    Text(name)
                                },
                                supportingContent = {
                                    if (!requiredVersion.isNullOrEmpty()) Text("${autoSelection.packageName} ${autoSelection.version}")
                                },
                                colors = transparentListItemColors
                            )
                        }
                    }
                }
            }
        }
    }
}