package app.revanced.manager.ui.screen.settings.update

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.BackgroundBundleUpdateTime
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.UpdatesSettingsViewModel
import app.revanced.manager.util.PermissionRequestHandler
import app.revanced.manager.util.hasNotificationPermission
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    var showBackgroundUpdateDialog by rememberSaveable { mutableStateOf(false) }

    if (showBackgroundUpdateDialog) {
        BackgroundBundleUpdateTimeDialog(
            onDismiss = { showBackgroundUpdateDialog = false },
            onConfirm = { vm.updateBackgroundBundleUpdateTime(it) }
        )
    }

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
                description = R.string.update_checking_manager_description
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.background_bundle_update),
                supportingContent = stringResource(R.string.background_bundle_update_description),
                modifier = Modifier.clickable {
                    showBackgroundUpdateDialog = true
                }
            )
        }
    }
}

@Composable
private fun BackgroundBundleUpdateTimeDialog(
    onDismiss: () -> Unit,
    onConfirm: (BackgroundBundleUpdateTime) -> Unit,
    prefs: PreferencesManager = koinInject()
) {
    var context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(prefs.backgroundBundleUpdateTime.getBlocking()) }

    var askNotificationPermission by rememberSaveable { mutableStateOf(false) }

    fun onApply() {
        onConfirm(selected)
        onDismiss()
    }

    if (askNotificationPermission) {
        PermissionRequestHandler(
            contract = ActivityResultContracts.RequestPermission(),
            input = Manifest.permission.POST_NOTIFICATIONS,
            title = stringResource(R.string.background_bundle_ask_notification),
            description = stringResource(R.string.background_bundle_ask_notification_description),
            icon = Icons.Outlined.Notifications,
            onDismissRequest = { askNotificationPermission = false },
            onResult = { granted ->
                askNotificationPermission = false
                onApply()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_radio_menu_title)) },
        text = {
            Column {
                BackgroundBundleUpdateTime.entries.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = it },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HapticRadioButton(
                            selected = selected == it,
                            onClick = { selected = it })
                        Text(stringResource(it.displayName))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selected != BackgroundBundleUpdateTime.NEVER &&
                        !hasNotificationPermission(context)
                        ) askNotificationPermission = true
                    else
                        onApply()
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}
