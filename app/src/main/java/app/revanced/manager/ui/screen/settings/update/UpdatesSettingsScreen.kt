package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
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
            SettingsListItem(
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        if (!vm.isConnected) {
                            context.toast(context.getString(R.string.no_network_toast))
                            return@launch
                        }
                        if (vm.checkForUpdates()) onUpdateClick()
                    }
                },
                headlineContent = stringResource(R.string.manual_update_check),
                supportingContent = stringResource(R.string.manual_update_check_description)
            )

            SettingsListItem(
                modifier = Modifier.clickable {
                    if (!vm.isConnected) {
                        context.toast(context.getString(R.string.no_network_toast))
                        return@clickable
                    }
                    onChangelogClick()
                },
                headlineContent = stringResource(R.string.changelog),
                supportingContent = stringResource(
                    R.string.changelog_description
                )
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
        }
    }
}