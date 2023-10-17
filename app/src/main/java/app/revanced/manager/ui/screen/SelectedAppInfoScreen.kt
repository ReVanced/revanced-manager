package app.revanced.manager.ui.screen

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.destination.SelectedAppInfoDestination
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.bundleInfoFlow
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import dev.olshevski.navigation.reimagined.AnimatedNavHost
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
    val bundles by remember(vm.selectedApp.packageName, vm.selectedApp.version) {
        vm.bundlesRepo.bundleInfoFlow(vm.selectedApp.packageName, vm.selectedApp.version)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allowExperimental by vm.prefs.allowExperimental.getAsState()

    val navController =
        rememberNavController<SelectedAppInfoDestination>(startDestination = SelectedAppInfoDestination.Main)

    AnimatedNavHost(controller = navController) { destination ->
        when (destination) {
            is SelectedAppInfoDestination.Main -> SelectedAppInfoScreen(
                onPatchClick = {
                    onPatchClick(
                        vm.selectedApp,
                        vm.selectionState.patches(bundles, allowExperimental),
                        vm.patchOptions
                    )
                },
                onSelectorClick = {
                    navController.navigate(
                        SelectedAppInfoDestination.PatchesSelector(
                            vm.selectedApp,
                            vm.getCustomPatchesOrNull(
                                bundles,
                                allowExperimental
                            ),
                            vm.patchOptions
                        )
                    )
                },
                onBackClick = onBackClick,
            )

            is SelectedAppInfoDestination.PatchesSelector -> PatchesSelectorScreen(
                onPatchClick = { patches, options ->
                    vm.setNullablePatches(patches)
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
    onSelectorClick: () -> Unit,
    onBackClick: () -> Unit,
    // vm: SelectedAppInfoViewModel,
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
            PageItem(R.string.patch, R.string.patcher, onPatchClick)
            PageItem(R.string.patches_selection, R.string.patcher, onSelectorClick)
        }
    }
}

@Composable
private fun PageItem(@StringRes title: Int, @StringRes description: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(description)) },
        trailingContent = {
            Icon(Icons.Outlined.ArrowRight, null)
        }
    )
}