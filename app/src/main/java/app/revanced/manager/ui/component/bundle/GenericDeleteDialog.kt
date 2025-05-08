package app.revanced.manager.ui.component.bundle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@Composable
fun GenericDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        title = title,
        icon = { Icon(Icons.Outlined.Delete, null) },
        text = description
    )
}