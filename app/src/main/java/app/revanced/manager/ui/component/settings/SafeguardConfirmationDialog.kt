package app.revanced.manager.ui.component.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.revanced.manager.R

@Composable
fun SafeguardConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    body: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        },
        icon = {
            Icon(Icons.Outlined.WarningAmber, null)
        },
        title = {
            Text(
                text = stringResource(id = R.string.warning),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )
        },
        text = {
            Text(body)
        }
    )
}
