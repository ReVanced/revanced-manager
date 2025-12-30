package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.viewmodel.VersionSelectorViewModel
import app.revanced.manager.util.enabled
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectorScreen(
    onBackClick: () -> Unit,
    onSave: (version: SelectedVersion) -> Unit,
    viewModel: VersionSelectorViewModel,
) {
    val versions by viewModel.availableVersions.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Select version") },
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.save)) },
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                onClick = { onSave(viewModel.selectedVersion) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            VersionOption(
                version = SelectedVersion.Auto,
                isSelected = viewModel.selectedVersion is SelectedVersion.Auto,
                onSelect = viewModel::selectVersion,
                title = { Text("Auto (Recommended)") },
                description = { Text("Automatically select the best available version") }
            )

            if (versions.isNotEmpty())
                HorizontalDivider()

            LazyColumn {
                items(versions, key = { it.first.version }) { version ->
                    VersionOption(
                        version = version.first,
                        isSelected = viewModel.selectedVersion == version.first,
                        onSelect = viewModel::selectVersion,
                        title = { Text(version.first.version) },
                        description = { Text(
                            "${version.second.let { if (it == 0) "No" else it }} incompatible patches")
                        }
                    )
                }
            }

            HorizontalDivider()

            VersionOption(
                version = SelectedVersion.Any,
                isSelected = viewModel.selectedVersion is SelectedVersion.Any,
                onSelect = viewModel::selectVersion,
                title = { Text("Any available version") },
                description = { Text("Use any available version regardless of compatibility") }
            )


        }
    }
}

@Composable
private fun VersionOption(
    version: SelectedVersion,
    isSelected: Boolean,
    onSelect: (SelectedVersion) -> Unit,
    title: @Composable (() -> Unit),
    description: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable { onSelect(version) },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        headlineContent = title,
        supportingContent = description,
        colors = transparentListItemColors
    )

}

@Composable
fun AppSourceSelectorDialog(
    plugins: List<LoadedDownloaderPlugin>,
    installedApp: Pair<SelectedApp.Installed, InstalledApp?>?,
    downloadedApps: List<DownloadedApp>,
    searchApp: SelectedApp.Search,
    activeSearchJob: String?,
    hasRoot: Boolean,
    requiredVersion: String?,
    onDismissRequest: () -> Unit,
    onSelectPlugin: (LoadedDownloaderPlugin) -> Unit,
    onSelect: (SelectedApp) -> Unit,
    onSelectDownloaded: (DownloadedApp) -> Unit = {},
) {
    val canSelect = activeSearchJob == null

    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.app_source_dialog_title)) },
        textHorizontalPadding = PaddingValues(horizontal = 0.dp),
        text = {
            Column {
                HorizontalDivider()
                LazyColumn {
                    item(key = "auto") {
                        val hasPlugins = plugins.isNotEmpty()
                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = canSelect && hasPlugins) { onSelect(searchApp) }
                                .enabled(hasPlugins),
                            headlineContent = { Text(stringResource(R.string.app_source_dialog_option_auto) + " (Recommended)") },
                            supportingContent = {
                                Text(
                                    "Automatically choose a suitable source"
// if (hasPlugins)
// stringResource(R.string.app_source_dialog_option_auto_description)
//// "Automatically choose a suitable source"
// else
// stringResource(R.string.app_source_dialog_option_auto_unavailable)
                                )
                            },
                            colors = transparentListItemColors
                        )
                    }

                    installedApp?.let { (app, meta) ->
                        item(key = "installed") {
                            val (usable, text) = when {
// Mounted apps must be unpatched before patching, which cannot be done without root access.
                                meta?.installType == InstallType.MOUNT && !hasRoot -> false to stringResource(
                                    R.string.app_source_dialog_option_installed_no_root
                                )
// Patching already patched apps is not allowed because patches expect unpatched apps.
                                meta?.installType == InstallType.DEFAULT -> false to stringResource(
                                    R.string.already_patched
                                )
// Version does not match suggested version.
                                requiredVersion != null && app.version != requiredVersion -> false to "Does not match the selected version"
// stringResource(
// R.string.app_source_dialog_option_installed_version_not_suggested,
// app.version
// )

                                else -> true to null
                            }
                            ListItem(
                                modifier = Modifier
                                    .clickable(enabled = canSelect && usable) { onSelect(app) }
                                    .enabled(usable),
                                overlineContent = { Text("Installed") },
                                headlineContent = { Text(app.version) },
                                supportingContent = text?.let { { Text(text) } },
                                colors = transparentListItemColors
                            )
                        }
                    }

                    items(downloadedApps, key = { it.version }) { app ->
                        val (usable, text) = when {
// Version does not match suggested version.
                            requiredVersion != null && app.version != requiredVersion -> false to "Does not match the selected version"
// stringResource(
// R.string.app_source_dialog_option_installed_version_not_suggested,
// app.version
// )

                            else -> true to null // "Downloaded using downloader plugin"
                        }
                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = usable) { onSelectDownloaded(app) }
                                .enabled(usable),
                            overlineContent = { Text("Downloaded") },
                            headlineContent = { Text(app.version) },
                            supportingContent = text?.let { { Text(text) } },
                            colors = transparentListItemColors
                        )
                    }

                    items(plugins, key = { "plugin_${it.packageName}" }) { plugin ->
                        ListItem(
                            modifier = Modifier.clickable(enabled = canSelect) {
                                onSelectPlugin(plugin)
                            },
                            overlineContent = { Text("Plugin") },
                            headlineContent = {
                                Text(
                                    plugin.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = (@Composable { LoadingIndicator() }).takeIf { activeSearchJob == plugin.packageName },
                            colors = transparentListItemColors
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    )
}

