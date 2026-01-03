package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.ui.component.AppTopBar
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Select source") },
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            HapticExtendedFloatingActionButton(
                text = { Text("Save") },
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = { onSave(viewModel.selectedSource) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Auto,
                onSelect = { viewModel.selectSource(SelectedSource.Auto) },
                headlineContent = { Text("Auto (Recommended)") },
                supportingContent = { Text("Automatically select the best available source") }
            )

            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Plugin("any"),
                onSelect = { viewModel.selectSource(SelectedSource.Plugin("any")) },
                headlineContent = { Text("Any available downloader") },
            )

            viewModel.installedVersion?.let { installedVersion ->
                HorizontalDivider()

                SourceOption(
                    isSelected = viewModel.selectedSource == SelectedSource.Installed,
                    onSelect = { viewModel.selectSource(SelectedSource.Installed) },
                    headlineContent = { Text(installedVersion) },
                    overlineContent = { Text("Installed") },
                    enabled = viewModel.input.version?.let { it == installedVersion } ?: true
                )
            }

            if (downloadedApps.isNotEmpty()) {
                HorizontalDivider()

                LazyColumn {
                    items(downloadedApps, key = { it.version }) { app ->
                        SourceOption(
                            isSelected = (viewModel.selectedSource as? SelectedSource.Downloaded)?.version == app.version,
                            onSelect = { viewModel.selectDownloadedApp(app) },
                            headlineContent = { Text(app.version) },
                            overlineContent = { Text("Downloaded") },
                        )
                    }
                }
            }

            HorizontalDivider()

            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Plugin("plugin-id"),
                onSelect = { viewModel.selectSource(SelectedSource.Plugin("plugin-id")) },
                headlineContent = { Text("APKMirror Downloader") },
                overlineContent = { Text("Plugin") },
            )

            SourceOption(
                isSelected = viewModel.selectedSource == SelectedSource.Plugin("another-plugin-id"),
                onSelect = { viewModel.selectSource(SelectedSource.Plugin("another-plugin-id")) },
                headlineContent = { Text("Another Plugin") },
                overlineContent = { Text("Plugin") },
                supportingContent = { Text("Untrusted") },
                enabled = false,
            )

        }
    }
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
