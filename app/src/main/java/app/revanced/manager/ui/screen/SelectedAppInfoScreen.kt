package app.revanced.manager.ui.screen

import android.content.pm.PackageInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.revanced.manager.ui.destination.SelectedAppInfoDestination
import app.revanced.manager.ui.model.BundleInfo.Extensions.bundleInfoFlow
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.toast
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SelectedAppInfoScreen(
    onPatchClick: (SelectedApp, PatchesSelection, Options) -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = LocalContext.current
    val bundles by remember(vm.selectedApp.packageName, vm.selectedApp.version) {
        vm.bundlesRepo.bundleInfoFlow(vm.selectedApp.packageName, vm.selectedApp.version)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allowExperimental by vm.prefs.allowExperimental.getAsState()
    val patches by remember {
        derivedStateOf {
            vm.getPatches(bundles, allowExperimental)
        }
    }
    val selectedPatchCount by remember {
        derivedStateOf {
            patches.values.sumOf { it.size }
        }
    }
    val availablePatchCount by remember {
        derivedStateOf {
            bundles.sumOf { it.patchCount }
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
                        vm.patchOptions
                    )
                },
                onPatchSelectorClick = {
                    navController.navigate(
                        SelectedAppInfoDestination.PatchesSelector(
                            vm.selectedApp,
                            vm.getCustomPatches(
                                bundles,
                                allowExperimental
                            ),
                            vm.patchOptions
                        )
                    )
                },
                onVersionSelectorClick = {
                    navController.navigate(SelectedAppInfoDestination.VersionSelector)
                },
                onBackClick = onBackClick,
                availablePatchCount = availablePatchCount,
                selectedPatchCount = selectedPatchCount,
                packageName = vm.selectedApp.packageName,
                version = vm.selectedApp.version,
                packageInfo = vm.selectedAppInfo,
            )

            is SelectedAppInfoDestination.VersionSelector -> VersionSelectorScreen(
                onBackClick = navController::pop,
                onAppClick = {
                    vm.selectedApp = it
                    navController.pop()
                },
                viewModel = getViewModel { parametersOf(vm.selectedApp.packageName) }
            )

            is SelectedAppInfoDestination.PatchesSelector -> PatchesSelectorScreen(
                onSave = { patches, options ->
                    vm.setCustomPatches(patches)
                    vm.patchOptions = options
                    navController.pop()
                },
                onBackClick = navController::pop,
                vm = getViewModel {
                    parametersOf(
                        PatchesSelectorViewModel.Params(
                            destination.app,
                            destination.currentSelection,
                            destination.options
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
    availablePatchCount: Int,
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppInfo(packageInfo, placeholderLabel = packageName) {
                Text(
                    stringResource(R.string.selected_app_meta, version, availablePatchCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PageItem(R.string.patch, stringResource(R.string.patch_item_description), onPatchClick)

            Text(
                stringResource(R.string.advanced),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

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
            Icon(Icons.Outlined.ArrowRight, null)
        }
    )
}