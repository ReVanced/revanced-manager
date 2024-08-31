package app.revanced.manager.ui.component.patcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType

@Composable
fun InstallPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (InstallType) -> Unit
) {
    var selectedInstallType by rememberSaveable { mutableStateOf(InstallType.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedInstallType)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.install_app))
            }
        },
        title = { Text(stringResource(R.string.select_install_type)) },
        text = {
            Column {
                InstallType.values().forEach {
                    ListItem(
                        modifier = Modifier.clickable { selectedInstallType = it },
                        leadingContent = {
                            RadioButton(
                                selected = selectedInstallType == it,
                                onClick = null
                            )
                        },
                        headlineContent = { Text(stringResource(it.stringResource)) }
                    )
                }
            }
        }
    )
}