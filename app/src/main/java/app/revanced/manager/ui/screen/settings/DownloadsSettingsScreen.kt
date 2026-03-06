package app.revanced.manager.ui.screen.settings

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.downloader.DownloaderPackageState
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.PillTab
import app.revanced.manager.ui.component.PillTabBar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private enum class DownloadsTab(
    val titleResId: Int,
    val icon: ImageVector
) {
    Downloaders(R.string.downloaders, Icons.Outlined.Download),
    Apps(R.string.tab_apps, Icons.Outlined.Apps)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalStdlibApi::class)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    onDownloaderClick: (String) -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val downloaderStates by viewModel.downloaderStates.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { DownloadsTab.entries.size })
    val scope = rememberCoroutineScope()
    val downloaderListState = rememberLazyListState()
    val appsListState = rememberLazyListState()
    val selectedListState = rememberSelectedListState(
        selectedPage = pagerState.currentPage,
        downloaderListState = downloaderListState,
        appsListState = appsListState
    )
    val canScroll by remember(selectedListState) {
        derivedStateOf {
            selectedListState.canScrollBackward || selectedListState.canScrollForward
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = { canScroll }
    )
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showCancelInstallConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val currentTab = DownloadsTab.entries[pagerState.currentPage]
    val isInstallingDownloader = viewModel.downloaderInstallState != DownloadsViewModel.DownloaderInstallState.IDLE

    val handleBack = {
        if (isInstallingDownloader) {
            showCancelInstallConfirmationDialog = true
        } else {
            onBackClick()
        }
    }

    // since we still want to have predictive back gesture at times when dialog isnt there
    PredictiveBackHandler(enabled = isInstallingDownloader) { progress ->
        try {
            progress.collect()
            showCancelInstallConfirmationDialog = true
        } catch (_: CancellationException) {}
    }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = { viewModel.deleteApps() },
            title = stringResource(R.string.downloader_delete_apps_title),
            description = stringResource(R.string.downloader_delete_apps_description),
            icon = Icons.Outlined.Delete
        )
    }

    if (showCancelInstallConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showCancelInstallConfirmationDialog = false },
            onConfirm = {
                viewModel.cancelDefaultDownloaderInstall()
                showCancelInstallConfirmationDialog = false
                onBackClick()
            },
            title = stringResource(R.string.cancel_downloader_install_title),
            description = stringResource(R.string.cancel_downloader_install_description),
            icon = Icons.Outlined.Download
        )
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.downloads)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (currentTab == DownloadsTab.Apps && viewModel.appSelection.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PillTabBar(
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                DownloadsTab.entries.forEachIndexed { index, tab ->
                    PillTab(
                        index = index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.titleResId)) },
                        icon = { Icon(tab.icon, null) }
                    )
                }
            }

            PullToRefreshBox(
                onRefresh = viewModel::refreshDownloaders,
                isRefreshing = viewModel.isRefreshingDownloaders,
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (DownloadsTab.entries[page]) {
                        DownloadsTab.Downloaders -> DownloadersTabContent(
                            downloaderStates = downloaderStates,
                            listState = downloaderListState,
                            viewModel = viewModel,
                            onDownloaderClick = onDownloaderClick,
                            onInstallDownloaderClick = viewModel::installDefaultDownloader
                        )

                        DownloadsTab.Apps -> AppsTabContent(
                            downloadedApps = downloadedApps,
                            listState = appsListState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSelectedListState(
    selectedPage: Int,
    downloaderListState: LazyListState,
    appsListState: LazyListState
) = remember(selectedPage, downloaderListState, appsListState) {
    if (selectedPage == DownloadsTab.Downloaders.ordinal) downloaderListState else appsListState
}

@Composable
private fun DownloadersTabContent(
    downloaderStates: Map<String, DownloaderPackageState>,
    listState: LazyListState,
    viewModel: DownloadsViewModel,
    onDownloaderClick: (String) -> Unit,
    onInstallDownloaderClick: () -> Unit
) {
    val installState = viewModel.downloaderInstallState

    Column(modifier = Modifier.fillMaxSize()) {
        if (downloaderStates.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyState(
                    icon = Icons.Outlined.Source,
                    title = R.string.no_downloaders_installed,
                    description = R.string.install_revanced_downloaders
                )
            }
        } else {
            LazyColumnWithScrollbar(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                downloaderStates.entries
                    .sortedBy { it.key }
                    .forEach { (packageName, state) ->
                        item(key = packageName) {
                            DownloaderItem(
                                packageName = packageName,
                                state = state,
                                viewModel = viewModel,
                                onClick = onDownloaderClick
                            )
                        }
                }
            }
        }

        if (downloaderStates.isEmpty()) {
            BottomContentBar {
                FilledTonalButton(
                    onClick = onInstallDownloaderClick,
                    enabled = installState == DownloadsViewModel.DownloaderInstallState.IDLE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (installState != DownloadsViewModel.DownloaderInstallState.IDLE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = stringResource(R.string.install_revanced_downloader)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            when (installState) {
                                DownloadsViewModel.DownloaderInstallState.DOWNLOADING -> R.string.api_downloader_downloading
                                DownloadsViewModel.DownloaderInstallState.INSTALLING -> R.string.api_downloader_installing
                                DownloadsViewModel.DownloaderInstallState.IDLE -> R.string.install_revanced_downloader
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsTabContent(
    downloadedApps: List<app.revanced.manager.data.room.apps.downloaded.DownloadedApp>,
    listState: LazyListState,
    viewModel: DownloadsViewModel
) {
    if (downloadedApps.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Download,
            title = R.string.downloader_settings_no_apps,
            description = R.string.downloader_settings_no_apps_description
        )
    } else {
        LazyColumnWithScrollbar(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            downloadedApps.forEach { app ->
                item(key = "${app.packageName}:${app.version}") {
                    val selected = app in viewModel.appSelection
                    ListItem(
                        modifier = Modifier.clickable { viewModel.toggleApp(app) },
                        headlineContent = { Text(app.packageName) },
                        supportingContent = { Text(app.version) },
                        leadingContent = (@Composable {
                            HapticCheckbox(
                                checked = selected,
                                onCheckedChange = { viewModel.toggleApp(app) }
                            )
                        }).takeIf { viewModel.appSelection.isNotEmpty() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloaderItem(
    packageName: String,
    state: DownloaderPackageState,
    viewModel: DownloadsViewModel,
    onClick: (String) -> Unit
) {
    val packageInfo = remember(packageName) { viewModel.pm.getPackageInfo(packageName) } ?: return

    ListItem(
        modifier = Modifier.clickable { onClick(packageName) },
        headlineContent = {
            AppLabel(
                packageInfo = packageInfo,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                stringResource(
                    when (state) {
                        is DownloaderPackageState.Loaded -> R.string.downloader_state_enabled
                        is DownloaderPackageState.Failed -> R.string.downloader_state_failed
                        is DownloaderPackageState.Untrusted -> R.string.downloader_state_disabled
                    }
                )
            )
        },
        leadingContent = {
            AppIcon(
                packageInfo = packageInfo,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        },
        trailingContent = { Text(packageInfo.versionName.orEmpty()) }
    )
}