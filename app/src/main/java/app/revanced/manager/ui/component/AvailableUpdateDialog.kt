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
import app.revanced.manager.ui.component.haptics.HapticCheckbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableUpdateDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    setShowManagerUpdateDialogOnLaunch: (Boolean) -> Unit,
    newVersion: String
) {
    var dontShowAgain by rememberSaveable { mutableStateOf(false) }
    val dismissDialog = {
        setShowManagerUpdateDialogOnLaunch(!dontShowAgain)
        onDismiss()
    }

    AlertDialogExtended(
        onDismissRequest = dismissDialog,
        confirmButton = {
            TextButton(
                onClick = {
                    dismissDialog()
                    onConfirm()
                }
            ) {
                Text(stringResource(R.string.show))
            }
        },
        dismissButton = {
            TextButton(
                onClick = dismissDialog
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
                            HapticCheckbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                        }
                    }
                )
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}
