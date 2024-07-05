package app.revanced.manager.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.domain.manager.base.BooleanPreference

@Composable
fun DangerousActionDialogBase(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    enableConfirmCountdown: BooleanPreference,
    @StringRes title: Int,
    body: String,
) {
    val enableCountdown by enableConfirmCountdown.getAsState()

    AlertDialog(
        onDismissRequest = onCancel,
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