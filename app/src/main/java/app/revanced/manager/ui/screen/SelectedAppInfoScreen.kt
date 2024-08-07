package app.revanced.manager.ui.screen

import android.content.pm.PackageInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
                onVersionSelectorClick = {
                    navController.navigate(SelectedAppInfoDestination.VersionSelector)
                },
                onBackClick = onBackClick,
                selectedPatchCount = selectedPatchCount,
                packageName = packageName,
                version = version,
                packageInfo = vm.selectedAppInfo,
            )

            is SelectedAppInfoDestination.VersionSelector -> VersionSelectorScreen(
                onBackClick = navController::pop,
                onAppClick = {
                    vm.selectedApp = it
                    navController.pop()
                },
                viewModel = koinViewModel { parametersOf(packageName) }
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
    onVersionSelectorClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedPatchCount: Int,
    packageName: String,
    version: String,
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
                    stringResource(R.string.selected_app_meta, version),
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
                stringResource(R.string.version_selector_item_description, version),
                onVersionSelectorClick
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