package app.revanced.manager.ui.component.bundle

import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.TextInputDialog

@Composable
fun BaseBundleDialog(
    modifier: Modifier = Modifier,
    isDefault: Boolean,
    name: String,
    onNameChange: ((String) -> Unit)? = null,
    remoteUrl: String?,
    onRemoteUrlChange: ((String) -> Unit)? = null,
    patchCount: Int,
    version: String?,
    autoUpdate: Boolean,
    onAutoUpdateChange: (Boolean) -> Unit,
    onPatchesClick: () -> Unit,
    onBundleTypeClick: () -> Unit = {},
    extraFields: @Composable ColumnScope.() -> Unit = {}
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(
            start = 8.dp,
            top = 8.dp,
            end = 4.dp,
        )
        .then(modifier)
) {
    var showNameInputDialog by rememberSaveable {
        mutableStateOf(false)
    }
    if (showNameInputDialog) {
        TextInputDialog(
            initial = name,
            title = stringResource(R.string.bundle_input_name),
            onDismissRequest = {
                showNameInputDialog = false
            },
            onConfirm = {
                showNameInputDialog = false
                onNameChange?.invoke(it)
            },
            validator = {
                it.length in 1..19
            }
        )
    }
    BundleListItem(
        headlineText = stringResource(R.string.bundle_input_name),
        supportingText = name.ifEmpty { stringResource(R.string.field_not_set) },
        modifier = Modifier.clickable(enabled = onNameChange != null) {
            showNameInputDialog = true
        }
    )

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
            headlineText = stringResource(R.string.automatically_update),
            supportingText = stringResource(R.string.automatically_update_description),
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

    BundleListItem(
        headlineText = stringResource(R.string.bundle_type),
        supportingText = stringResource(R.string.bundle_type_description),
        modifier = Modifier.clickable {
            onBundleTypeClick()
        }
    ) {
        FilledTonalButton(
            onClick = onBundleTypeClick,
            content = {
                if (remoteUrl == null) {
                    Text(stringResource(R.string.local))
                } else {
                    Text(stringResource(R.string.remote))
                }
            }
        )
    }

    if (version != null || patchCount > 0) {
        Text(
            text = stringResource(R.string.information),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    BundleListItem(
        headlineText = stringResource(R.string.patches),
        supportingText = if (patchCount == 0) stringResource(R.string.no_patches)
        else stringResource(R.string.patches_available, patchCount),
        modifier = Modifier.clickable(enabled = patchCount > 0) {
            onPatchesClick()
        }
    ) {
        if (patchCount > 0) {
            Icon(
                Icons.Outlined.ArrowRight,
                stringResource(R.string.patches)
            )
        }
    }

    version?.let {
        BundleListItem(
            headlineText = stringResource(R.string.version),
            supportingText = it,
        )
    }
}