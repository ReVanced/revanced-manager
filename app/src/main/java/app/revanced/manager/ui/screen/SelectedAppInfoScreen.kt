package app.revanced.manager.ui.screen

import android.content.pm.PackageInfo
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.destination.SelectedAppInfoDestination
import app.revanced.manager.ui.model.BundleInfo.Extensions.bundleInfoFlow
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

    var showSourceSelectorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val navController =
        rememberNavController<SelectedAppInfoDestination>(startDestination = SelectedAppInfoDestination.Main)

    NavBackHandler(controller = navController)

    AnimatedNavHost(controller = navController) { destination ->
        when (destination) {
            is SelectedAppInfoDestination.Main -> SelectedAppInfoScreen(
                onPatchClick = patchClick@{
                    if (selectedPatchCount == 0) {
                        context.toast(context.getString(R.string.no_patches_selected))

                        return@patchClick
                    }
                    onPatchClick(
                        vm.selectedApp,
                        patches,
                        vm.getOptionsFiltered(bundles)
                    )
                },
                onPatchSelectorClick = {
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
                },
                onSourceSelectorClick = {
                    showSourceSelectorDialog = true
                    // navController.navigate(SelectedAppInfoDestination.VersionSelector)
                },
                onBackClick = onBackClick,
                selectedPatchCount = selectedPatchCount,
                packageName = packageName,
                version = version,
                packageInfo = vm.selectedAppInfo,
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedAppInfoScreen(
    onPatchClick: () -> Unit,
    onPatchSelectorClick: () -> Unit,
    onSourceSelectorClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedPatchCount: Int,
    packageName: String,
    version: String?,
    packageInfo: PackageInfo?,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_info),
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        stringResource(R.string.patch)
                    )
                },
                onClick = onPatchClick
            )
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppInfo(packageInfo, placeholderLabel = packageName) {
                Text(
                    version ?: stringResource(R.string.selected_app_meta_any_version),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PageItem(
                R.string.patch_selector_item,
                stringResource(R.string.patch_selector_item_description, selectedPatchCount),
                onPatchSelectorClick
            )
            PageItem(
                R.string.version_selector_item,
                version?.let { stringResource(R.string.version_selector_item_description, it) }
                    ?: stringResource(R.string.version_selector_item_description_auto),
                onSourceSelectorClick
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
private fun AppSourceSelectorDialog(onDismissRequest: () -> Unit) {
    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {

                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text("Select source") },
        textHorizontalPadding = PaddingValues(horizontal = 0.dp),
        text = {
            /*
            val presets = remember(scope.option.presets) {
                scope.option.presets?.entries?.toList().orEmpty()
            }

            LazyColumn {
                @Composable
                fun Item(title: String, value: Any?, presetKey: String?) {
                    ListItem(
                        modifier = Modifier.clickable { selectedPreset = presetKey },
                        headlineContent = { Text(title) },
                        supportingContent = value?.toString()?.let { { Text(it) } },
                        leadingContent = {
                            RadioButton(
                                selected = selectedPreset == presetKey,
                                onClick = { selectedPreset = presetKey }
                            )
                        },
                        colors = transparentListItemColors
                    )
                }

                items(presets, key = { it.key }) {
                    Item(it.key, it.value, it.key)
                }

                item(key = null) {
                    Item(stringResource(R.string.option_preset_custom_value), null, null)
                }
            }
             */
        }
    )
}