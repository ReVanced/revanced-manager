package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SafeguardDialog
import app.revanced.manager.util.transparentListItemColors

@Composable
fun UniversalPatchWarningDialog(onDismiss: () -> Unit) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.warning,
        body = stringResource(R.string.universal_patch_warning_description),
    )
}

@Composable
fun SelectionWarningDialog(onDismiss: () -> Unit) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.warning,
        body = stringResource(R.string.selection_warning_description),
    )
}

@Composable
fun IncompatiblePatchesDialog(
    appVersion: String,
    onDismissRequest: () -> Unit
) = WarningAlertDialog(
    onDismissRequest = onDismissRequest,
    title = stringResource(R.string.incompatible_patches),
    text = stringResource(R.string.incompatible_patches_dialog, appVersion)
)

@Composable
fun IncompatiblePatchDialog(
    appVersion: String,
    compatibleVersions: List<String>,
    onDismissRequest: () -> Unit
) = WarningAlertDialog(
    onDismissRequest = onDismissRequest,
    title = stringResource(R.string.incompatible_patch),
    text = stringResource(
        R.string.app_version_not_compatible,
        appVersion,
        compatibleVersions.joinToString(", ")
    )
)

@Composable
fun ScopeDialog(
    bundleName: String,
    onDismissRequest: () -> Unit,
    onAllPatches: () -> Unit,
    onBundleOnly: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.scope_dialog_title)) },
    confirmButton = {
        TextButton(onClick = onAllPatches) {
            Text(stringResource(R.string.scope_all_patches))
        }
    },
    dismissButton = {
        TextButton(onClick = onBundleOnly) {
            Text(stringResource(R.string.scope_bundle_patches, bundleName))
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
    patch: PatchInfo,
    onDismissRequest: () -> Unit,
    values: Map<String, Any?>? = null,
    reset: (() -> Unit)? = null,
    set: ((String, Any?) -> Unit)? = null,
    selectionWarningEnabled: Boolean = false
) = FullscreenDialog(onDismissRequest = onDismissRequest) {
    val readOnly = set == null

    Scaffold(
        topBar = {
            AppTopBar(
                title = patch.name,
                onBackClick = onDismissRequest,
                actions = {
                    if (reset != null) {
                        IconButton(onClick = reset) {
                            Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(patch.options.orEmpty(), key = { it.name }) { option ->
                if (readOnly) {
                    ListItem(
                        headlineContent = { Text(option.name) },
                        supportingContent = { Text(option.description) },
                        trailingContent = {
                            Text(
                                text = option.default?.toString()
                                    ?: stringResource(R.string.option_default_value_none),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = transparentListItemColors
                    )
                } else {
                    val name = option.name
                    val value =
                        if (values == null || !values.contains(name)) option.default else values[name]

                    @Suppress("UNCHECKED_CAST")
                    OptionItem(
                        option = option as Option<Any>,
                        value = value,
                        setValue = { set(name, it) },
                        selectionWarningEnabled = selectionWarningEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String
) = AlertDialog(
    icon = { Icon(Icons.Outlined.WarningAmber, null) },
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(title) },
    text = { Text(text) }
)
