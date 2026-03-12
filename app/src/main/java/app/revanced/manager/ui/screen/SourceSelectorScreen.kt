package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.GroupHeader
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
    val allDownloaderSections by viewModel.downloaderSections.collectAsStateWithLifecycle(emptyList())
    val (downloaderSections, unavailableSections) = allDownloaderSections.partition { section ->
        section.options.all { it.disableReason == null }
    }
    val unavailableDownloaders = unavailableSections.flatMap { it.options }

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
        ColumnWithScrollbar(modifier = Modifier.padding(paddingValues)) {
            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Auto,
                onSelect = { viewModel.selectSource(SelectedSource.Auto) },
                headlineContent = { Text(stringResource(R.string.version_selector_auto_title)) },
                supportingContent = { Text(stringResource(R.string.source_selector_auto_description)) },
            )
            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Downloader(),
                onSelect = { viewModel.selectSource(SelectedSource.Downloader()) },
                headlineContent = { Text(stringResource(R.string.source_selector_any_available_downloader)) },
            )

            viewModel.localApp?.let { option ->
                GroupHeader(stringResource(R.string.source_selector_category_local))
                SourceOption(option, viewModel.selectedSource, viewModel::selectSource)
            }

            viewModel.installedSource?.let { option ->
                GroupHeader(stringResource(R.string.installed))
                SourceOption(option, viewModel.selectedSource, viewModel::selectSource)
            }

            if (downloadedApps.isNotEmpty()) {
                GroupHeader(stringResource(R.string.source_selector_category_downloaded))
                downloadedApps.forEach { option ->
                    SourceOption(option, viewModel.selectedSource, viewModel::selectSource)
                }
            }

            downloaderSections.forEach { section ->
                GroupHeader(section.title)
                section.options.forEach { option ->
                    SourceOption(option, viewModel.selectedSource, viewModel::selectSource)
                }
            }

            if (unavailableDownloaders.isNotEmpty()) {
                GroupHeader(stringResource(R.string.downloaders))
                unavailableDownloaders.forEach { option ->
                    SourceOption(option, viewModel.selectedSource, viewModel::selectSource)
                }
            }
        }
    }
}

@Composable
private fun SourceOption(
    sourceOption: SourceSelectorViewModel.SourceOption,
    selectedSource: SelectedSource,
    onSelect: (SelectedSource) -> Unit,
) {
    SourceOption(
        isSelected = selectedSource == sourceOption.source,
        onSelect = { onSelect(sourceOption.source) },
        headlineContent = { Text(sourceOption.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = sourceOption.disableReason?.let { { Text(stringResource(it.message)) } },
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
