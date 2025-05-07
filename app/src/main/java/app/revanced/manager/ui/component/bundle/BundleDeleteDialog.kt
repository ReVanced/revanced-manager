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
fun BundleDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    bundleName: String? = null
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
        title = { Text(stringResource(R.string.delete_bundle_dialog_title)) },
        icon = { Icon(Icons.Outlined.Delete, null) },
        text = { Text(
            if (bundleName != null)
                stringResource(R.string.delete_bundle_single_dialog_description, bundleName)
            else
                stringResource(R.string.delete_bundle_multiple_dialog_description)
        ) }
    )
}