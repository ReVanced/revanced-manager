package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.enabled
import app.revanced.manager.util.toast
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
    val context = LocalContext.current
    val resources = LocalResources.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkConnected = remember { networkInfo.isConnected() }
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by vm.bundleInfoFlow.collectAsStateWithLifecycle(emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val patches by remember {
        derivedStateOf {
            vm.getPatches(bundles, allowIncompatiblePatches)
        }
    }
    val selectedPatchCount = patches.values.sumOf { it.size }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handleDownloaderActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        launcher.launch(intent)
    }
    val composableScope = rememberCoroutineScope()

    val error by vm.errorFlow.collectAsStateWithLifecycle(null)

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
            if (error != null) return@Scaffold

            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        stringResource(R.string.patch)
                    )
                },
                onClick = patchClick@{
                    if (selectedPatchCount == 0) {
                        context.toast(resources.getString(R.string.no_patches_selected))

                        return@patchClick
                    }

                    composableScope.launch {
                        if (!vm.hasSetRequiredOptions(patches)) {
                            onRequiredOptions(
                                vm.selectedApp,
                                vm.getCustomPatches(bundles, allowIncompatiblePatches),
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
        val downloaders by vm.downloaders.collectAsStateWithLifecycle(emptyList())

        if (vm.showSourceSelector) {
            val requiredVersion by vm.requiredVersion.collectAsStateWithLifecycle(null)

            AppSourceSelectorDialog(
                downloaders = downloaders,
                installedApp = vm.installedAppData,
                searchApp = SelectedApp.Search(
                    vm.packageName,
                    vm.desiredVersion
                ),
                activeSearchJob = vm.activeDownloader,
                hasRoot = vm.hasRoot,
                onDismissRequest = vm::dismissSourceSelector,
                onSelectDownloader = vm::searchUsingDownloader,
                requiredVersion = requiredVersion,
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
                onClick = {
                    onPatchSelectorClick(
                        vm.selectedApp,
                        vm.getCustomPatches(
                            bundles,
                            allowIncompatiblePatches
                        ),
                        vm.options
                    )
                }
            )
            PageItem(
                R.string.apk_source_selector_item,
                when (val app = vm.selectedApp) {
                    is SelectedApp.Search -> stringResource(R.string.apk_source_auto)
                    is SelectedApp.Installed -> stringResource(R.string.apk_source_installed)
                    is SelectedApp.Download -> stringResource(
                        R.string.apk_source_downloader,
                        downloaders.find { it.packageName == app.data.downloaderPackageName && it.name == app.data.downloaderClassName }?.name
                            ?: app.data.downloaderPackageName
                    )

                    is SelectedApp.Local -> stringResource(R.string.apk_source_local)
                },
                onClick = {
                    vm.showSourceSelector()
                }
            )
            error?.let {
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

                when {
                    !needsInternet -> {}
                    !networkConnected -> {
                        NotificationCard(
                            isWarning = true,
                            icon = Icons.Outlined.WarningAmber,
                            text = stringResource(R.string.network_unavailable_warning),
                            onDismiss = null
                        )
                    }

                    networkMetered -> {
                        NotificationCard(
                            isWarning = true,
                            icon = Icons.Outlined.WarningAmber,
                            text = stringResource(R.string.network_metered_warning),
                            onDismiss = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageItem(@StringRes title: Int, description: String, onClick: () -> Unit) {
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
            Text(
                description,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppSourceSelectorDialog(
    downloaders: List<LoadedDownloader>,
    installedApp: Pair<SelectedApp.Installed, InstalledApp?>?,
    searchApp: SelectedApp.Search,
    activeSearchJob: LoadedDownloader?,
    hasRoot: Boolean,
    requiredVersion: String?,
    onDismissRequest: () -> Unit,
    onSelectDownloader: (LoadedDownloader) -> Unit,
    onSelect: (SelectedApp) -> Unit,
) {
    val canSelect = activeSearchJob == null

    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.app_source_dialog_title)) },
        textHorizontalPadding = PaddingValues(horizontal = 0.dp),
        text = {
            LazyColumn {
                item(key = "auto") {
                    val hasDownloader = downloaders.isNotEmpty()
                    ListItem(
                        modifier = Modifier
                            .clickable(enabled = canSelect && hasDownloader) { onSelect(searchApp) }
                            .enabled(hasDownloader),
                        headlineContent = { Text(stringResource(R.string.app_source_dialog_option_auto)) },
                        supportingContent = {
                            Text(
                                if (hasDownloader)
                                    stringResource(R.string.app_source_dialog_option_auto_description)
                                else
                                    stringResource(R.string.app_source_dialog_option_auto_unavailable)
                            )
                        },
                        colors = transparentListItemColors
                    )
                }

                installedApp?.let { (app, meta) ->
                    item(key = "installed") {
                        val (usable, text) = when {
                            // Mounted apps must be unpatched before patching, which cannot be done without root access.
                            meta?.installType == InstallType.MOUNT && !hasRoot -> false to stringResource(
                                R.string.app_source_dialog_option_installed_no_root
                            )
                            // Patching already patched apps is not allowed because patches expect unpatched apps.
                            meta?.installType == InstallType.DEFAULT -> false to stringResource(R.string.already_patched)
                            // Version does not match suggested version.
                            requiredVersion != null && app.version != requiredVersion -> false to stringResource(
                                R.string.app_source_dialog_option_installed_version_not_suggested,
                                app.version
                            )

                            else -> true to app.version
                        }
                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = canSelect && usable) { onSelect(app) }
                                .enabled(usable),
                            headlineContent = { Text(stringResource(R.string.installed)) },
                            supportingContent = { Text(text) },
                            colors = transparentListItemColors
                        )
                    }
                }

                items(
                    downloaders,
                    key = { "downloader_${it.packageName}_${it.className}" }) { downloader ->
                    ListItem(
                        modifier = Modifier.clickable(enabled = canSelect) {
                            onSelectDownloader(
                                downloader
                            )
                        },
                        headlineContent = { Text(downloader.name) },
                        trailingContent = (@Composable { LoadingIndicator() }).takeIf { activeSearchJob == downloader },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    )
}