package app.revanced.manager.ui.component.bundle

import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.TextInputDialog

@Composable
fun BaseBundleDialog(
    modifier: Modifier = Modifier,
    isDefault: Boolean,
    name: String?,
    onNameChange: ((String) -> Unit)? = null,
    remoteUrl: String?,
    onRemoteUrlChange: ((String) -> Unit)? = null,
    patchCount: Int,
    version: String?,
    autoUpdate: Boolean,
    onAutoUpdateChange: (Boolean) -> Unit,
    onPatchesClick: () -> Unit,
    extraFields: @Composable ColumnScope.() -> Unit = {}
) {
    ColumnWithScrollbar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (name != null) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.bundle_input_name)) },
                value = name,
                onValueChange = {
                    if (it.length in 1..19) onNameChange?.invoke(it)
                },
            )
        }

        remoteUrl?.takeUnless { isDefault }?.let { url ->
            var showUrlInputDialog by rememberSaveable {
                mutableStateOf(false)
            }
            if (showUrlInputDialog) {
                TextInputDialog(
                    initial = url,
                    title = stringResource(R.string.bundle_input_source_url),
                    onDismissRequest = { showUrlInputDialog = false },
                    onConfirm = {
                        showUrlInputDialog = false
                        onRemoteUrlChange?.invoke(it)
                    },
                    validator = {
                        if (it.isEmpty()) return@TextInputDialog false

                        URLUtil.isValidUrl(it)
                    }
                )
            }

            BundleListItem(
                modifier = Modifier.clickable(enabled = onRemoteUrlChange != null) {
                    showUrlInputDialog = true
                },
                headlineText = stringResource(R.string.bundle_input_source_url),
                supportingText = url.ifEmpty { stringResource(R.string.field_not_set) }
            )
        }

        extraFields()

        if (remoteUrl != null) {
            BundleListItem(
                headlineText = stringResource(R.string.bundle_auto_update),
                supportingText = stringResource(R.string.bundle_auto_update_description),
                trailingContent = {
                    Switch(
                        checked = autoUpdate,
                        onCheckedChange = onAutoUpdateChange
                    )
                },
                modifier = Modifier.clickable {
                    onAutoUpdateChange(!autoUpdate)
                }
            )
        }

        OutlinedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.bundle_information))
                version?.let {
                    BundleInfoItem(
                        text = { Text(it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        icon = Icons.Outlined.Sell
                    )
                }
                BundleInfoItem(
                    text = {
                        if (remoteUrl == null) {
                            Text(stringResource(R.string.local),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.remote),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    icon = Icons.Outlined.Folder
                )
                patchCount.let {
                    BundleInfoItem(
                        text = {
                            Text(
                                pluralStringResource(
                                    R.plurals.bundle_patches_available,
                                    it,
                                    it
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = Icons.Outlined.Extension
                    )
                }
            }
        }
    }
}

@Composable
fun BundleInfoItem(
    text: @Composable () -> Unit,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        text()
    }
}