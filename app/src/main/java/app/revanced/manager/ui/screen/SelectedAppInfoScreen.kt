package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.destination.SelectedAppInfoDestination
import app.revanced.manager.ui.model.BundleInfo.Extensions.bundleInfoFlow
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.enabled
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import dev.olshevski.navigation.reimagined.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAppInfoScreen(
    onPatchClick: (SelectedApp, PatchSelection, Options) -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = LocalContext.current

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by remember(packageName, version) {
        vm.bundlesRepo.bundleInfoFlow(packageName, version)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val patches by remember {
        derivedStateOf {
            vm.getPatches(bundles, allowIncompatiblePatches)
        }
    }
    val selectedPatchCount by remember {
        derivedStateOf {
            patches.values.sumOf { it.size }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handlePluginActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        launcher.launch(intent)
    }

    val navController =
        rememberNavController<SelectedAppInfoDestination>(startDestination = SelectedAppInfoDestination.Main)

    NavBackHandler(controller = navController)

    AnimatedNavHost(controller = navController) { destination ->
        val error by vm.errorFlow.collectAsStateWithLifecycle(null)
        when (destination) {
            is SelectedAppInfoDestination.Main -> Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(R.string.app_info),
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
                                context.toast(context.getString(R.string.no_patches_selected))

                                return@patchClick
                            }
                            onPatchClick(
                                vm.selectedApp,
                                patches,
                                vm.getOptionsFiltered(bundles)
                            )
                        }
                    )
                }
            ) { paddingValues ->
                val plugins by vm.plugins.collectAsStateWithLifecycle(emptyList())

                if (vm.showSourceSelector) {
                    val requiredVersion by vm.requiredVersion.collectAsStateWithLifecycle(null)

                    AppSourceSelectorDialog(
                        plugins = plugins,
                        installedApp = vm.installedAppData,
                        searchApp = SelectedApp.Search(
                            vm.packageName,
                            vm.desiredVersion
                        ),
                        activeSearchJob = vm.activePluginAction,
                        hasRoot = vm.hasRoot,
                        onDismissRequest = vm::dismissSourceSelector,
                        onSelectPlugin = vm::searchUsingPlugin,
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
                            navController.navigate(
                                SelectedAppInfoDestination.PatchesSelector(
                                    vm.selectedApp,
                                    vm.getCustomPatches(
                                        bundles,
                                        allowIncompatiblePatches
                                    ),
                                    vm.options
                                )
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
                                plugins.find { it.packageName == app.data.pluginPackageName }?.name
                                    ?: app.data.pluginPackageName
                            )

                            is SelectedApp.Local -> stringResource(R.string.apk_source_local)
                        },
                        onClick = {
                            vm.showSourceSelector()
                        }
                    )
                    error?.let {
                        Text(
                            stringResource(it.resourceId),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            is SelectedAppInfoDestination.PatchesSelector -> PatchesSelectorScreen(
                onSave = { patches, options ->
                    vm.updateConfiguration(patches, options, bundles)
                    navController.pop()
                },
                onBackClick = navController::pop,
                vm = koinViewModel {
                    parametersOf(
                        PatchesSelectorViewModel.Params(
                            destination.app,
                            destination.currentSelection,
                            destination.options,
                        )
                    )
                }
            )
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

@Composable
private fun AppSourceSelectorDialog(
    plugins: List<LoadedDownloaderPlugin>,
    installedApp: Pair<SelectedApp.Installed, InstalledApp?>?,
    searchApp: SelectedApp.Search,
    activeSearchJob: String?,
    hasRoot: Boolean,
    requiredVersion: String?,
    onDismissRequest: () -> Unit,
    onSelectPlugin: (LoadedDownloaderPlugin) -> Unit,
    onSelect: (SelectedApp) -> Unit,
) {
    val canSelect = activeSearchJob == null

    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.app_source_dialog_title)) },
        textHorizontalPadding = PaddingValues(horizontal = 0.dp),
        text = {
            LazyColumn {
                item(key = "auto") {
                    val hasPlugins = plugins.isNotEmpty()
                    ListItem(
                        modifier = Modifier
                            .clickable(enabled = canSelect && hasPlugins) { onSelect(searchApp) }
                            .enabled(hasPlugins),
                        headlineContent = { Text(stringResource(R.string.app_source_dialog_option_auto)) },
                        supportingContent = {
                            Text(
                                if (hasPlugins)
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

                items(plugins, key = { "plugin_${it.packageName}" }) { plugin ->
                    ListItem(
                        modifier = Modifier.clickable(enabled = canSelect) { onSelectPlugin(plugin) },
                        headlineContent = { Text(plugin.name) },
                        trailingContent = (@Composable { LoadingIndicator() }).takeIf { activeSearchJob == plugin.packageName },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    )
}