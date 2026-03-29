package app.revanced.manager.ui.component.selectedapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.enabled
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppSourceSelectorDialog(
    downloaders: Map<String, List<LoadedDownloader>>,
    downloadedApps: List<SelectedApp.Local>,
    activeSearchJob: LoadedDownloader?,
    requiredVersion: String?,
    autoSelection: SelectedApp,
    onDismissRequest: () -> Unit,
    onSelectAuto: () -> Unit,
    onSelectDownloader: (LoadedDownloader) -> Unit,
    onSelectFromStorage: () -> Unit,
    onSelect: (SelectedApp) -> Unit,
) {
    val canSelect = activeSearchJob == null

    val hasDownloaded = downloadedApps.any {
        requiredVersion == null || it.version == requiredVersion
    }
    val hasAutoSource =
        downloaders.isNotEmpty() ||
                hasDownloaded ||
                autoSelection is SelectedApp.Installed

    FullscreenDialog(onDismissRequest) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.app_source_dialog_title),
                    onBackClick = onDismissRequest
                )
            }
        ) { paddingValues ->
            LazyColumn(Modifier.padding(paddingValues)) {
                item("auto") {
                    ListItem(
                        modifier = Modifier
                            .clickable(
                                enabled = canSelect && hasAutoSource,
                                onClick = onSelectAuto
                            )
                            .enabled(hasAutoSource),
                        headlineContent = {
                            Text(stringResource(R.string.app_source_dialog_option_auto))
                        },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (hasAutoSource)
                                        R.string.app_source_dialog_option_auto_description
                                    else
                                        R.string.app_source_dialog_option_auto_unavailable
                                )
                            )
                        },
                        colors = transparentListItemColors
                    )
                }

                item("storage") {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onSelectFromStorage),
                        headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                        supportingContent = { Text(stringResource(R.string.select_from_storage_description)) },
                        colors = transparentListItemColors
                    )
                }

                if (downloadedApps.isNotEmpty()) {
                    item { HorizontalDivider() }

                    items(downloadedApps, key = { it.version }) { app ->
                        val usable =
                            requiredVersion == null || app.version == requiredVersion

                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = canSelect && usable) { onSelect(app) }
                                .enabled(usable),
                            headlineContent = { Text(app.packageName) },
                            supportingContent = { Text(app.version) },
                            overlineContent = {
                                Text(stringResource(R.string.source_selector_category_downloaded))
                            },
                            colors = transparentListItemColors
                        )
                    }
                }

                if (downloaders.isNotEmpty()) {
                    item { HorizontalDivider() }

                    downloaders.forEach { (name, list) ->
                        items(list) { downloader ->
                            ListItem(
                                modifier = Modifier.clickable(enabled = canSelect) {
                                    onSelectDownloader(downloader)
                                },
                                headlineContent = { Text(downloader.name) },
                                trailingContent = {
                                    if (activeSearchJob == downloader) {
                                        LoadingIndicator()
                                    }
                                },
                                overlineContent = {
                                    Text(name)
                                },
                                supportingContent = {
                                    if (!requiredVersion.isNullOrEmpty()) Text("${autoSelection.packageName} ${autoSelection.version}")
                                },
                                colors = transparentListItemColors
                            )
                        }
                    }
                }
            }
        }
    }
}