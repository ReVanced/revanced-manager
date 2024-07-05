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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.revanced.manager.R
import app.revanced.manager.domain.manager.base.BooleanPreference

@Composable
fun DangerousActionDialog(
    onCancel: () -> Unit,
    confirmButton: @Composable () -> Unit,
    @StringRes title: Int,
    body: String,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
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
fun DangerousActionDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    enableConfirmCountdown: BooleanPreference,
    @StringRes title: Int,
    body: String,
) {
    val enableCountdown by enableConfirmCountdown.getAsState()

    DangerousActionDialog(
        onCancel = onCancel,
        confirmButton = {
            Countdown(start = if (enableCountdown) 3 else 0) { timer ->
                LaunchedEffect(timer) {
                    if (timer == 0) enableConfirmCountdown.update(false)
                }

                TextButton(
                    onClick = onConfirm,
                    enabled = timer == 0
                ) {
                    if (timer == 0)
                        Text(stringResource(R.string.continue_), color = MaterialTheme.colorScheme.error)
                    else
                        Text(stringResource(R.string.continue_countdown, timer))
                }
            }
        },
        title = title,
        body = body
    )
}