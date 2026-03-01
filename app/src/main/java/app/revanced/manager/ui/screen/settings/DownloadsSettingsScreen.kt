package app.revanced.manager.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.downloader.DownloaderPackageState
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import org.koin.androidx.compose.koinViewModel
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalStdlibApi::class)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val downloaderStates by viewModel.downloaderStates.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val canScroll by remember {
        derivedStateOf {
            listState.canScrollBackward || listState.canScrollForward
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = { canScroll }
    )
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
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.downloads)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (viewModel.appSelection.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteApps() }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        ),
    ) { paddingValues ->
        PullToRefreshBox(
            onRefresh = viewModel::refreshDownloaders,
            isRefreshing = viewModel.isRefreshingDownloaders,
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    ListSection(
                        title = stringResource(R.string.downloaders),
                        leadingContent = { Icon(Icons.Outlined.Source, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
                        if (downloaderStates.isEmpty()) {
                            Text(
                                stringResource(R.string.no_downloaders_installed),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            downloaderStates.forEach { (packageName, state) ->
                                DownloaderItem(
                                    context = context,
                                    viewModel = viewModel,
                                    packageName = packageName,
                                    state = state
                                )
                            }
                        }
                    }
                }

                item {
                    ListSection(
                        title = stringResource(R.string.downloaded_apps),
                        leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
                        if (downloadedApps.isEmpty()) {
                            Text(
                                stringResource(R.string.downloader_settings_no_apps),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            downloadedApps.forEach { app ->
                                val selected = app in viewModel.appSelection

                                SettingsListItem(
                                    onClick = { viewModel.toggleApp(app) },
                                    headlineContent = app.packageName,
                                    leadingContent = (@Composable {
                                        HapticCheckbox(
                                            checked = selected,
                                            onCheckedChange = { viewModel.toggleApp(app) }
                                        )
                                    }).takeIf { viewModel.appSelection.isNotEmpty() },
                                    supportingContent = app.version,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun DownloaderItem(
    context: android.content.Context,
    viewModel: DownloadsViewModel,
    packageName: String,
    state: DownloaderPackageState
) {
    var showDialog by rememberSaveable {
        mutableStateOf(false)
    }

    fun dismiss() {
        showDialog = false
    }

    val packageInfo =
        remember(packageName) {
            viewModel.pm.getPackageInfo(packageName)
        } ?: return

    if (showDialog) {
        val signature =
            remember(packageName) {
                val androidSignature = viewModel.pm.getSignature(packageName)
                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(androidSignature.toByteArray())
                hash.toHexString(format = HexFormat.UpperCase)
            }
        val appName = remember {
            packageInfo.applicationInfo?.loadLabel(context.packageManager)
                ?.toString()
                ?: packageName
        }

        when (state) {
            is DownloaderPackageState.Loaded -> TrustDialog(
                title = R.string.downloader_revoke_trust_dialog_title,
                body = stringResource(
                    R.string.downloader_trust_dialog_body,
                    packageName,
                    signature
                ),
                downloaderName = appName,
                signature = signature,
                onDismiss = ::dismiss,
                onConfirm = {
                    viewModel.revokeDownloaderTrust(packageName)
                    dismiss()
                }
            )

            is DownloaderPackageState.Failed -> ExceptionViewerDialog(
                text = remember(state.throwable) {
                    state.throwable.stackTraceToString()
                },
                onDismiss = ::dismiss
            )

            is DownloaderPackageState.Untrusted -> TrustDialog(
                title = R.string.downloader_trust_dialog_title,
                body = stringResource(
                    R.string.downloader_trust_dialog_body
                ),
                downloaderName = appName,
                signature = signature,
                onDismiss = ::dismiss,
                onConfirm = {
                    viewModel.trustDownloader(packageName)
                    dismiss()
                }
            )
        }

        SettingsListItem(
            onClick = { showDialog = true },
            headlineContent = {
                AppLabel(
                    packageInfo = packageInfo,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = stringResource(
                when (state) {
                    is DownloaderPackageState.Loaded -> R.string.downloader_state_trusted
                    is DownloaderPackageState.Failed -> R.string.downloader_state_failed
                    is DownloaderPackageState.Untrusted -> R.string.downloader_state_untrusted
                }
            ),
            trailingContent = { Text(packageInfo.versionName!!) }
        )
    }
}

@Composable
private fun TrustDialog(
    @StringRes title: Int,
    body: String,
    downloaderName: String,
    signature: String,
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
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(body)
                Card {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(
                                R.string.downloader_trust_dialog_name,
                                downloaderName
                            ),
                        )
                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Text(
                                stringResource(
                                    R.string.downloader_trust_dialog_signature,
                                    signature.chunked(2).joinToString(" ")
                                ), modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}