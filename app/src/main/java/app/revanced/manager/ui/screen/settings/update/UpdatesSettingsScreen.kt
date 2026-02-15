package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.UpdatesSettingsViewModel
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesSettingsScreen(
    onBackClick: () -> Unit,
    onChangelogClick: () -> Unit,
    onUpdateClick: () -> Unit,
    vm: UpdatesSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.updates),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListSection(
                title = stringResource(R.string.manager),
                leadingContent = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                SettingsListItem(
                    headlineContent = stringResource(R.string.manual_update_check),
                    supportingContent = stringResource(R.string.manual_update_check_description),
                    onClick = {
                        coroutineScope.launch {
                            if (!vm.isConnected) {
                                context.toast(resources.getString(R.string.no_network_toast))
                                return@launch
                            }
                            if (vm.checkForUpdates()) onUpdateClick()
                        }
                    }
                )

                SettingsListItem(
                    headlineContent = stringResource(R.string.changelog),
                    supportingContent = stringResource(R.string.changelog_description),
                    onClick = {
                        if (!vm.isConnected) {
                            context.toast(resources.getString(R.string.no_network_toast))
                            return@SettingsListItem
                        }
                        onChangelogClick()
                    }
                )

                BooleanItem(
                    preference = vm.managerAutoUpdates,
                    headline = R.string.update_checking_manager,
                    description = R.string.update_checking_manager_description
                )

                BooleanItem(
                    preference = vm.showManagerUpdateDialogOnLaunch,
                    headline = R.string.show_manager_update_dialog_on_launch,
                    description = R.string.show_manager_update_dialog_on_launch_description
                )

                BooleanItem(
                    preference = vm.useManagerPrereleases,
                    headline = R.string.manager_prereleases,
                    description = R.string.manager_prereleases_description
                )
            }
        }
    }
}