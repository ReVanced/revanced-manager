package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.viewmodel.SourceSelectorViewModel
import app.revanced.manager.util.enabled
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectorScreen(
    onBackClick: () -> Unit,
    onSave: (source: SelectedSource) -> Unit,
    viewModel: SourceSelectorViewModel,
) {
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val plugins by viewModel.plugins.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Select source") },
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.save)) },
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = { onSave(viewModel.selectedSource) },
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar (
            contentPadding = paddingValues,
        ) {
            item {
                SourceOption(
                    isSelected = viewModel.selectedSource == SelectedSource.Auto,
                    onSelect = { viewModel.selectSource(SelectedSource.Auto) },
                    headlineContent = { Text("Auto (Recommended)") },
                    supportingContent = { Text("Automatically select the best available source") }
                )
            }
            item {
                SourceOption(
                    isSelected = viewModel.selectedSource == SelectedSource.Plugin(null),
                    onSelect = { viewModel.selectSource(SelectedSource.Plugin(null)) },
                    headlineContent = { Text("Any available downloader") },
                )
            }

            viewModel.localApp?.let { option ->
                item {
                    HorizontalDivider()
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }

            viewModel.installedSource?.let { option ->
                item {
                    HorizontalDivider()
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }

            if (downloadedApps.isNotEmpty()) item { HorizontalDivider() }
            items(downloadedApps, key = { it.key }) { option ->
                SourceOption(
                    sourceOption = option,
                    isSelected = viewModel.selectedSource == option.source,
                    onSelect = viewModel::selectSource,
                )
            }

            if (plugins.isNotEmpty()) item { HorizontalDivider() }
            items(plugins, key = { it.key }) { option ->
                SourceOption(
                    sourceOption = option,
                    isSelected = viewModel.selectedSource == option.source,
                    onSelect = viewModel::selectSource,
                )
            }
        }
    }
}

@Composable
private fun SourceOption(
    sourceOption: SourceSelectorViewModel.SourceOption,
    isSelected: Boolean,
    onSelect: (SelectedSource) -> Unit,
) = SourceOption(
    isSelected = isSelected,
    onSelect = { onSelect(sourceOption.source) },
    overlineContent = sourceOption.category?.let {{ Text(it) }},
    headlineContent = { Text(sourceOption.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    supportingContent = sourceOption.disableReason?.let {{ Text(it.message) }},
    enabled = sourceOption.disableReason == null,
)

@Composable
private fun SourceOption(
    isSelected: Boolean,
    onSelect: () -> Unit,
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled) { onSelect() }
            .enabled(enabled),
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        overlineContent = overlineContent,
        colors = transparentListItemColors
    )
}
