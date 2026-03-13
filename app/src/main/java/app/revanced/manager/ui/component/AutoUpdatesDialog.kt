package app.revanced.manager.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.util.transparentListItemColors

@Composable
fun AutoUpdatesDialog(onSubmit: (Boolean) -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = { onSubmit(enabled) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(false) }) {
                Text(stringResource(R.string.no))
            }
        },
        icon = { Icon(Icons.Outlined.Update, null) },
        title = { Text(text = stringResource(R.string.auto_updates_dialog_title)) },
        text = { Text(text = stringResource(R.string.auto_updates_dialog_description)) }
    )
}
