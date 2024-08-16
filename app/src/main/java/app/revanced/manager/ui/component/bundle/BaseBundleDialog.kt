package app.revanced.manager.ui.component.bundle

import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.TextInputDialog

@Composable
fun BaseBundleDialog(
    modifier: Modifier = Modifier,
    isDefault: Boolean,
    name: String?,
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
            .then(modifier),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(800)),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp)
            ) {
                version?.let {
                    Tag(Icons.Outlined.Sell, it)
                }
                Tag(Icons.Outlined.Extension, patchCount.toString())
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

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
                modifier = Modifier.clickable(
                    enabled = onRemoteUrlChange != null,
                    onClick = {
                        showUrlInputDialog = true
                    }
                ),
                headlineText = stringResource(R.string.bundle_input_source_url),
                supportingText = url.ifEmpty {
                    stringResource(R.string.field_not_set)
                }
            )
        }

        val patchesClickable = patchCount > 0
        BundleListItem(
            headlineText = stringResource(R.string.patches),
            supportingText = stringResource(R.string.bundle_view_patches),
            modifier = Modifier.clickable(
                enabled = patchesClickable,
                onClick = onPatchesClick
            )
        ) {
            if (patchesClickable) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowRight,
                    stringResource(R.string.patches)
                )
            }
        }

        extraFields()
    }
}

@Composable
private fun Tag(
    icon: ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}