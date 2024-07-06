package app.revanced.manager.ui.component

import androidx.annotation.StringRes
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
fun SafeguardDialog(
    onDismiss: () -> Unit,
    @StringRes title: Int,
    body: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        icon = {
            Icon(Icons.Outlined.WarningAmber, null)
        },
        title = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )
        },
        text = {
            Text(body)
        }
    )
}

@Composable
fun NonSuggestedVersionDialog(suggestedVersion: String, onDismiss: () -> Unit) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.non_suggested_version_warning_title,
        body = stringResource(R.string.non_suggested_version_warning_description, suggestedVersion),
    )
}