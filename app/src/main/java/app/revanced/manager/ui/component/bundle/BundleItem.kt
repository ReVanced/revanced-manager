package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.nameState
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BundleItem(
    bundle: PatchBundleSource,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    selectable: Boolean,
    onSelect: () -> Unit,
    isBundleSelected: Boolean,
    toggleSelection: (Boolean) -> Unit,
) {
    var viewBundleDialogPage by rememberSaveable { mutableStateOf(false) }
    val state by bundle.state.collectAsStateWithLifecycle()

    val version by remember(bundle) {
        bundle.propsFlow().map { props -> props?.versionInfo?.patches }
    }.collectAsStateWithLifecycle(null)
    val name by bundle.nameState

    if (viewBundleDialogPage) {
        BundleInformationDialog(
            onDismissRequest = { viewBundleDialogPage = false },
            onDeleteRequest = {
                viewBundleDialogPage = false
                onDelete()
            },
            bundle = bundle,
            onUpdate = onUpdate,
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
                Checkbox(
                    checked = isBundleSelected,
                    onCheckedChange = toggleSelection,
                )
            }
        } else null,

        headlineContent = { Text(name) },
        supportingContent = {
            state.patchBundleOrNull()?.patches?.size?.let { patchCount ->
                Text(pluralStringResource(R.plurals.patch_count, patchCount, patchCount))
            }
        },
        trailingContent = {
            Row {
                val icon = remember(state) {
                    when (state) {
                        is PatchBundleSource.State.Failed -> Icons.Outlined.ErrorOutline to R.string.bundle_error
                        is PatchBundleSource.State.Missing -> Icons.Outlined.Warning to R.string.bundle_missing
                        is PatchBundleSource.State.Loaded -> null
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

                version?.let { Text(text = it) }
            }
        },
    )
}