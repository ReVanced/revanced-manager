package app.revanced.manager.ui.component.bundle

import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchBundleManifestAttributes
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.haptics.HapticSwitch

@Composable
fun BaseBundleDialog(
    modifier: Modifier = Modifier,
    isDefault: Boolean,
    remoteUrl: String?,
    onRemoteUrlChange: ((String) -> Unit)? = null,
    patchCount: Int,
    version: String?,
    autoUpdate: Boolean,
    bundleManifestAttributes: PatchBundleManifestAttributes?,
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
            version?.let {
                Tag(Icons.Outlined.Sell, it)
            }
            bundleManifestAttributes?.description?.let {
                Tag(Icons.Outlined.Description, it)
            }
            bundleManifestAttributes?.source?.let {
                Tag(Icons.Outlined.Commit, it)
            }
            bundleManifestAttributes?.author?.let {
                Tag(Icons.Outlined.Person, it)
            }
            bundleManifestAttributes?.contact?.let {
                Tag(Icons.AutoMirrored.Outlined.Send, it)
            }
            bundleManifestAttributes?.website?.let {
                Tag(Icons.Outlined.Language, it, isUrl = true)
            }
            bundleManifestAttributes?.license?.let {
                Tag(Icons.Outlined.Gavel, it)
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
                    HapticSwitch(
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
            headlineText = stringResource(R.string.bundle_view_patches),
            supportingText = stringResource(R.string.bundle_view_all_patches, patchCount),
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
    text: String,
    isUrl: Boolean = false
) {
    val uriHandler = LocalUriHandler.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isUrl) {
            Modifier
                .clickable {
                    try {
                        uriHandler.openUri(text)
                    } catch (_: Exception) {}
                }
        }
        else
            Modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if(isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}