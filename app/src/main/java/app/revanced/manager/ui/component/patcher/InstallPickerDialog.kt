package app.revanced.manager.ui.component.patcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallPickerDialog(
    installTypes: List<InstallType>,
    isAdbConnected: Boolean,
    onRefreshAdb: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (InstallType) -> Unit
) {
    var selectedInstallType by rememberSaveable { mutableStateOf(InstallType.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedInstallType)
                    onDismiss()
                },
                enabled = selectedInstallType != InstallType.ADB || isAdbConnected,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.install_app))
            }
        },
        title = { Text(stringResource(R.string.select_install_type)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                installTypes.forEach {
                    ListItem(
                        modifier = Modifier.clickable { selectedInstallType = it },
                        leadingContent = {
                            HapticRadioButton(
                                selected = selectedInstallType == it,
                                onClick = null
                            )
                        },
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(
                                        if (it == InstallType.ADB && !isAdbConnected) R.string.adb_disconnected
                                        else it.stringResource
                                    )
                                )
                                if (it == InstallType.ADB && !isAdbConnected) {
                                    TooltipIconButton(
                                        onClick = onRefreshAdb,
                                        tooltip = stringResource(R.string.adb_setup)
                                    ) { contentDescription ->
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = contentDescription,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    )
}