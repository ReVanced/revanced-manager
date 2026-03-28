package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.settings.BooleanItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsFilterBottomSheet(
    onDismissRequest: () -> Unit,
    showPatched: Boolean,
    onToggleShowPatched: (Boolean) -> Unit,
    showInstalled: Boolean,
    onToggleShowInstalled: (Boolean) -> Unit,
    showNotInstalled: Boolean,
    onToggleShowNotInstalled: (Boolean) -> Unit,
    showSystem: Boolean,
    onToggleShowSystem: (Boolean) -> Unit,
    applyFilterToPinned: Boolean,
    onToggleApplyFilterToPinned: (Boolean) -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.patch_selector_sheet_filter_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.apps),
                    style = MaterialTheme.typography.titleMedium
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CheckedFilterChip(
                        selected = showPatched,
                        onClick = { onToggleShowPatched(!showPatched) },
                        label = { Text(stringResource(R.string.patched_apps_section_title)) }
                    )

                    CheckedFilterChip(
                        selected = showInstalled,
                        onClick = { onToggleShowInstalled(!showInstalled) },
                        label = { Text(stringResource(R.string.installed)) }
                    )

                    CheckedFilterChip(
                        selected = showNotInstalled,
                        onClick = { onToggleShowNotInstalled(!showNotInstalled) },
                        label = { Text(stringResource(R.string.not_installed)) }
                    )

                    CheckedFilterChip(
                        selected = showSystem,
                        onClick = { onToggleShowSystem(!showSystem) },
                        label = { Text(stringResource(R.string.system)) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            BooleanItem(
                value = applyFilterToPinned,
                onValueChange = onToggleApplyFilterToPinned,
                headline = R.string.apply_to_pinned
            )
        }
    }
}
