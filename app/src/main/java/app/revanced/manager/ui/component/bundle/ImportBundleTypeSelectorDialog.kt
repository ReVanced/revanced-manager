package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.model.BundleType

@Composable
fun ImportBundleTypeSelectorDialog(
    onDismiss: () -> Unit,
    onConfirm: (BundleType) -> Unit,
) {
    var bundleType: BundleType by rememberSaveable { mutableStateOf(BundleType.Remote) }

    AlertDialogExtended(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(bundleType) }
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
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.select_bundle_type_dialog_description)
                )
                Column {
                    ListItem(
                        modifier = Modifier.clickable(
                            role = Role.RadioButton,
                            onClick = { bundleType = BundleType.Remote }
                        ),
                        headlineContent = { Text(stringResource(R.string.remote)) },
                        overlineContent = { Text(stringResource(R.string.recommended)) },
                        supportingContent = { Text(stringResource(R.string.remote_bundle_description)) },
                        leadingContent = {
                            RadioButton(
                                selected = bundleType == BundleType.Remote,
                                onClick = null
                            )
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        modifier = Modifier.clickable(
                            role = Role.RadioButton,
                            onClick = { bundleType = BundleType.Local }
                        ),
                        headlineContent = { Text(stringResource(R.string.local)) },
                        supportingContent = { Text(stringResource(R.string.local_bundle_description)) },
                        overlineContent = { }, // we're using this parameter to force the 3-line ListItem state
                        leadingContent = {
                            RadioButton(
                                selected = bundleType == BundleType.Local,
                                onClick = null
                            )
                        }
                    )
                }
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}
