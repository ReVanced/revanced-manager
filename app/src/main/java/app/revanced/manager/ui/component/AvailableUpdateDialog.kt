package app.revanced.manager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableUpdateDialog(
    onDismiss: () -> Unit, onConfirm: () -> Unit, setShowManagerUpdateDialog: (Boolean) -> Unit, newVersion: String
) {
    var dontShowAgain by rememberSaveable { mutableStateOf(false) }

    AlertDialogExtended(
        onDismissRequest = {
            setShowManagerUpdateDialog(!dontShowAgain)
            onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    setShowManagerUpdateDialog(!dontShowAgain)
                    onDismiss()
                    onConfirm()
                }
            ) {
                Text(stringResource(R.string.show))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    setShowManagerUpdateDialog(!dontShowAgain)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.dismiss))
            }
        },
        icon = {
            Icon(imageVector = Icons.Outlined.Update, contentDescription = null)
        },
        title = {
            Text(stringResource(R.string.update_available))
        },
        text = {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.update_available_dialog_description, newVersion)
                )
                ListItem(
                    modifier = Modifier.clickable { dontShowAgain = !dontShowAgain },
                    headlineContent = {
                        Text(stringResource(R.string.never_show_again))
                    },
                    leadingContent = {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                        }
                    }
                )
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}
