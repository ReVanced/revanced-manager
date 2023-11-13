package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.model.BundleType

@Composable
fun ImportBundleTypeSelectorDialog(
    onDismiss: () -> Unit,
    onConfirm: (BundleType) -> Unit,
) {
    var bundleType: BundleType by rememberSaveable { mutableStateOf(BundleType.Remote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { bundleType.let(onConfirm) }
            ) {
                Text(stringResource(R.string.select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.select_bundle_type_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(stringResource(R.string.select_bundle_type_dialog_description))
                Column {
                    ListItem(
                        modifier = Modifier.clickable { bundleType = BundleType.Local },
                        headlineContent = { Text(stringResource(R.string.local)) },
                        supportingContent = { Text(stringResource(R.string.local_bundle_description)) },
                        leadingContent = {
                            RadioButton(
                                selected = bundleType == BundleType.Local,
                                onClick = { bundleType = BundleType.Local })
                        }
                    )
                    Divider()
                    ListItem(
                        modifier = Modifier.clickable { bundleType = BundleType.Remote },
                        headlineContent = { Text(stringResource(R.string.remote)) },
                        overlineContent = { Text(stringResource(R.string.recommended)) },
                        supportingContent = { Text(stringResource(R.string.remote_bundle_description)) },
                        leadingContent = {
                            RadioButton(
                                selected = bundleType == BundleType.Remote,
                                onClick = { bundleType = BundleType.Remote })
                        }
                    )
                }
            }
        }
    )
}