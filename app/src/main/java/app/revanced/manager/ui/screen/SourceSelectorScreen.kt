package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.revanced.manager.ui.component.GroupHeader
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
    val downloaderSections by viewModel.downloaderSections.collectAsStateWithLifecycle(emptyList())
    val unavailableDownloaders by viewModel.unavailableDownloaders.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.app_source_dialog_title)) },
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
                    headlineContent = { Text(stringResource(R.string.version_selector_auto_title)) },
                    supportingContent = { Text(stringResource(R.string.source_selector_auto_description)) },
                )
            }
            item {
                SourceOption(
                    isSelected = viewModel.selectedSource == SelectedSource.Downloader(),
                    onSelect = { viewModel.selectSource(SelectedSource.Downloader()) },
                    headlineContent = { Text(stringResource(R.string.source_selector_any_available_downloader)) },
                )
            }

            viewModel.localApp?.let { option ->
                item {
                    GroupHeader(stringResource(R.string.source_selector_category_local))
                }
                item {
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }

            viewModel.installedSource?.let { option ->
                item {
                    GroupHeader(stringResource(R.string.installed))
                }
                item {
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }

            if (downloadedApps.isNotEmpty()) {
                item {
                    GroupHeader(stringResource(R.string.source_selector_category_downloaded))
                }
            }
            items(downloadedApps, key = { it.key }) { option ->
                SourceOption(
                    sourceOption = option,
                    isSelected = viewModel.selectedSource == option.source,
                    onSelect = viewModel::selectSource,
                )
            }

            downloaderSections.forEach { section ->
                item(key = "downloader_header_${section.key}") {
                    GroupHeader(section.title)
                }
                items(section.options, key = { it.key }) { option ->
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }

            if (unavailableDownloaders.isNotEmpty()) {
                item(key = "downloader_unavailable_header") {
                    GroupHeader(stringResource(R.string.downloaders))
                }
                items(unavailableDownloaders, key = { it.key }) { option ->
                    SourceOption(
                        sourceOption = option,
                        isSelected = viewModel.selectedSource == option.source,
                        onSelect = viewModel::selectSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceOption(
    sourceOption: SourceSelectorViewModel.SourceOption,
    isSelected: Boolean,
    onSelect: (SelectedSource) -> Unit,
) {
    SourceOption(
        isSelected = isSelected,
        onSelect = { onSelect(sourceOption.source) },
        headlineContent = { Text(sourceOption.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = sourceOption.disableReason?.let {{ Text(stringResource(it.message)) }},
        enabled = sourceOption.disableReason == null,
    )
}

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
