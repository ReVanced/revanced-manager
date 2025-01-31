package app.revanced.manager.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import org.koin.androidx.compose.koinViewModel
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val pullRefreshState = rememberPullToRefreshState()
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val pluginStates by viewModel.downloaderPluginStates.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

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
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = viewModel.isRefreshingPlugins
            )
        }

        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullToRefresh(
                    isRefreshing = viewModel.isRefreshingPlugins,
                    state = pullRefreshState,
                    onRefresh = viewModel::refreshPlugins
                )
        ) {
            item {
                GroupHeader(stringResource(R.string.downloader_plugins))
            }
            pluginStates.forEach { (packageName, state) ->
                item(key = packageName) {
                    var showDialog by rememberSaveable {
                        mutableStateOf(false)
                    }

                    fun dismiss() {
                        showDialog = false
                    }

                    val packageInfo =
                        remember(packageName) {
                            viewModel.pm.getPackageInfo(
                                packageName
                            )
                        } ?: return@item

                    if (showDialog) {
                        val signature =
                            remember(packageName) {
                                val androidSignature =
                                    viewModel.pm.getSignature(packageName)
                                val hash = MessageDigest.getInstance("SHA-256")
                                    .digest(androidSignature.toByteArray())
                                hash.toHexString(format = HexFormat.UpperCase)
                            }

                        when (state) {
                            is DownloaderPluginState.Loaded -> TrustDialog(
                                title = R.string.downloader_plugin_revoke_trust_dialog_title,
                                body = stringResource(
                                    R.string.downloader_plugin_trust_dialog_body,
                                    packageName,
                                    signature
                                ),
                                onDismiss = ::dismiss,
                                onConfirm = {
                                    viewModel.revokePluginTrust(packageName)
                                    dismiss()
                                }
                            )

                            is DownloaderPluginState.Failed -> ExceptionViewerDialog(
                                text = remember(state.throwable) {
                                    state.throwable.stackTraceToString()
                                },
                                onDismiss = ::dismiss
                            )

                            is DownloaderPluginState.Untrusted -> TrustDialog(
                                title = R.string.downloader_plugin_trust_dialog_title,
                                body = stringResource(
                                    R.string.downloader_plugin_trust_dialog_body,
                                    packageName,
                                    signature
                                ),
                                onDismiss = ::dismiss,
                                onConfirm = {
                                    viewModel.trustPlugin(packageName)
                                    dismiss()
                                }
                            )
                        }
                    }

                    SettingsListItem(
                        modifier = Modifier.clickable { showDialog = true },
                        headlineContent = {
                            AppLabel(
                                packageInfo = packageInfo,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        supportingContent = stringResource(
                            when (state) {
                                is DownloaderPluginState.Loaded -> R.string.downloader_plugin_state_trusted
                                is DownloaderPluginState.Failed -> R.string.downloader_plugin_state_failed
                                is DownloaderPluginState.Untrusted -> R.string.downloader_plugin_state_untrusted
                            }
                        ),
                        trailingContent = { Text(packageInfo.versionName!!) }
                    )
                }
            }
            if (pluginStates.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.downloader_no_plugins_installed),
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

@Composable
private fun TrustDialog(
    @StringRes title: Int,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.continue_))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        },
        title = { Text(stringResource(title)) },
        text = { Text(body) }
    )
}