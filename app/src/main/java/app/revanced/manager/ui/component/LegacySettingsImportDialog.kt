package app.revanced.manager.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.revanced.manager.R

@Composable
fun LegacySettingsImportDialog(onDismiss: () -> Unit, onSubmit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.legacy_settings_import_dialog_dismiss))
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit
            ) {
                Text(stringResource(R.string.import_))
            }
        },
        icon = {
            Icon(Icons.Outlined.Update, null)
        },
        title = {
            Text(
                text = stringResource(R.string.legacy_settings_import_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.legacy_settings_import_dialog_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    )
}