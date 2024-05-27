package app.revanced.manager.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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

@Composable
fun AutoUpdatesDialog(onSubmit: (Boolean, Boolean) -> Unit) {
    var patchesEnabled by rememberSaveable { mutableStateOf(true) }
    var managerEnabled by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = { onSubmit(managerEnabled, patchesEnabled) }) {
                Text(stringResource(R.string.save))
            }
        },
        icon = { Icon(Icons.Outlined.Update, null) },
        title = { Text(text = stringResource(R.string.auto_updates_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.auto_updates_dialog_description))

                Column {
                    AutoUpdatesItem(
                        headline = R.string.auto_updates_dialog_manager,
                        icon = Icons.Outlined.Update,
                        checked = managerEnabled,
                        onCheckedChange = { managerEnabled = it }
                    )
                    HorizontalDivider()
                    AutoUpdatesItem(
                        headline = R.string.auto_updates_dialog_patches,
                        icon = Icons.Outlined.Source,
                        checked = patchesEnabled,
                        onCheckedChange = { patchesEnabled = it }
                    )
                }

                Text(text = stringResource(R.string.auto_updates_dialog_note))
            }
        }
    )
}

@Composable
private fun AutoUpdatesItem(
    @StringRes headline: Int,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) = ListItem(
    leadingContent = { Icon(icon, null) },
    headlineContent = { Text(stringResource(headline)) },
    trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
    modifier = Modifier.clickable { onCheckedChange(!checked) }
)