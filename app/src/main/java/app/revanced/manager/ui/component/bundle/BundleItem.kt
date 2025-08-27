package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.haptics.HapticCheckbox

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BundleItem(
    src: PatchBundleSource,
    patchCount: Int,
    selectable: Boolean,
    isBundleSelected: Boolean,
    toggleSelection: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
) {
    var viewBundleDialogPage by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (viewBundleDialogPage) {
        BundleInformationDialog(
            src = src,
            patchCount = patchCount,
            onDismissRequest = { viewBundleDialogPage = false },
            onDeleteRequest = { showDeleteConfirmationDialog = true },
            onUpdate = onUpdate,
        )
    }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                onDelete()
                viewBundleDialogPage = false
            },
            title = stringResource(R.string.delete),
            description = stringResource(R.string.patches_delete_single_dialog_description, src.name),
            icon = Icons.Outlined.Delete
        )
    }

    ListItem(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { viewBundleDialogPage = true },
                onLongClick = onSelect,
            ),
        leadingContent = if (selectable) {
            {
                HapticCheckbox(
                    checked = isBundleSelected,
                    onCheckedChange = toggleSelection,
                )
            }
        } else null,

        headlineContent = { Text(src.name) },
        supportingContent = {
            if (src.state is PatchBundleSource.State.Available) {
                Text(pluralStringResource(R.plurals.patch_count, patchCount, patchCount))
            }
        },
        trailingContent = {
            Row {
                val icon = remember(src.state) {
                    when (src.state) {
                        is PatchBundleSource.State.Failed -> Icons.Outlined.ErrorOutline to R.string.patches_error
                        is PatchBundleSource.State.Missing -> Icons.Outlined.Warning to R.string.patches_missing
                        is PatchBundleSource.State.Available -> null
                    }
                }

                icon?.let { (vector, description) ->
                    Icon(
                        vector,
                        contentDescription = stringResource(description),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                src.version?.let { Text(text = it) }
            }
        },
    )
}