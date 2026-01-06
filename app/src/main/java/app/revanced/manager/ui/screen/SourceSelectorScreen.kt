package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.network.downloader.DownloaderPluginState
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
    val context = LocalContext.current

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
                text = { Text("Save") },
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = { onSave(viewModel.selectedSource) },
            )
        }
    ) { paddingValues ->
        LazyColumn (
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

            viewModel.installedVersion?.let { installedVersion ->
                item {
                    HorizontalDivider()

                    SourceOption(
                        isSelected = viewModel.selectedSource == SelectedSource.Installed,
                        onSelect = { viewModel.selectSource(SelectedSource.Installed) },
                        headlineContent = { Text(installedVersion) },
                        overlineContent = { Text("Installed") },
                        enabled = viewModel.input.version?.let { it == installedVersion } ?: true
                    )
                }
            }

            if (downloadedApps.isNotEmpty()) item { HorizontalDivider() }

            items(downloadedApps, key = { it.version }) { app ->
                SourceOption(
                    isSelected = (viewModel.selectedSource as? SelectedSource.Downloaded)?.version == app.version,
                    onSelect = { viewModel.selectDownloadedApp(app) },
                    headlineContent = { Text(app.version) },
                    overlineContent = { Text("Downloaded") },
                )
            }

            if (plugins.isNotEmpty()) item { HorizontalDivider() }

            items(plugins, key = { it.first }) {
                val packageInfo = remember {
                    viewModel.getPackageInfo(it.first)
                }

                val label = remember {
                    packageInfo?.applicationInfo?.loadLabel(context.packageManager).toString()
                }

                SourceOption(
                    isSelected = viewModel.selectedSource == SelectedSource.Plugin(it.first),
                    onSelect = { viewModel.selectSource(SelectedSource.Plugin(it.first)) },
                    headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    overlineContent = { Text("Plugin") },
                    enabled = it.second is DownloaderPluginState.Loaded,
                    supportingContent = (it.second as? DownloaderPluginState.Untrusted)?.let { {
                        Text("Not trusted")
                    } }
                )
            }
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
