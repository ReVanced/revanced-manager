package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.network.downloader.DownloaderPackage
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val downloaderSources by viewModel.downloaderSources.collectAsStateWithLifecycle(emptyMap())
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = { viewModel.deleteApps() },
            title = stringResource(R.string.downloader_delete_apps_title),
            description = stringResource(R.string.downloader_delete_apps_description),
            icon = Icons.Outlined.Delete
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.downloads),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                actions = {
                    if (viewModel.appSelection.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteApps() }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        PullToRefreshBox(
            onRefresh = viewModel::refreshDownloaders,
            isRefreshing = viewModel.isRefreshingDownloaders,
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    GroupHeader(stringResource(R.string.downloaders), modifier = Modifier.clickable { viewModel.refreshDownloaders() }) // TODO: implement UI. This should not be clickable.
                }

                item {
                    BooleanItem(
                        preference = viewModel.usePrereleases,
                        coroutineScope = viewModel.viewModelScope,
                        headline = R.string.downloader_prereleases,
                        description = R.string.downloader_prereleases_description,
                    )
                }

                downloaderSources.forEach { (uid, source) ->
                    item(key = uid) {
                        var showDialog by rememberSaveable {
                            mutableStateOf(false)
                        }

                        fun dismiss() {
                            showDialog = false
                        }

                        if (showDialog) {
                            (source.state as? Source.State.Failed)?.let {
                                ExceptionViewerDialog(
                                    text = remember(it.throwable) {
                                        it.throwable.stackTraceToString()
                                    },
                                    onDismiss = ::dismiss
                                )
                            }
                        }

                        SettingsListItem(
                            modifier = Modifier.clickable(enabled = source.state is Source.State.Missing) { showDialog = true },
                            headlineContent = {
                                Text(
                                    source.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            supportingContent = when (source.state) {
                                is Source.State.Available<*> -> StringBuilder(stringResource(R.string.downloader_state_loaded)).apply {
                                    val downloaderPkg = source.state.obj as DownloaderPackage
                                    val names = downloaderPkg.downloaders.joinToString("\n") { it.name }
                                    if (names.isNotEmpty()) append("\n\n$names")
                                }.toString()

                                is Source.State.Failed -> stringResource(R.string.downloader_state_failed)
                                is Source.State.Missing -> stringResource(R.string.downloader_state_missing)
                            },
                            // TODO: version maybe?
                            // trailingContent = { Text(source.version) }
                        )
                    }
                }

                if (downloaderSources.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_downloader_installed),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    GroupHeader(stringResource(R.string.downloaded_apps))
                }
                items(downloadedApps, key = { it.packageName to it.version }) { app ->
                    val selected = app in viewModel.appSelection

                    SettingsListItem(
                        modifier = Modifier.clickable { viewModel.toggleApp(app) },
                        headlineContent = app.packageName,
                        leadingContent = (@Composable {
                            HapticCheckbox(
                                checked = selected,
                                onCheckedChange = { viewModel.toggleApp(app) }
                            )
                        }).takeIf { viewModel.appSelection.isNotEmpty() },
                        supportingContent = app.version,
                        tonalElevation = if (selected) 8.dp else 0.dp
                    )
                }
                if (downloadedApps.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.downloader_settings_no_apps),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}