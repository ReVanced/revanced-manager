package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.domain.sources.Source.State
import app.revanced.manager.network.downloader.DownloaderPackage
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.PillTab
import app.revanced.manager.ui.component.PillTabBar
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.sources.ImportSourceDialog
import app.revanced.manager.ui.component.sources.ImportSourceDialogStrings
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import app.revanced.manager.util.relativeTime
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale.getDefault

private enum class DownloadsTab(
    val titleResId: Int,
    val icon: ImageVector
) {
    Downloaders(R.string.downloaders, Icons.Outlined.Download),
    Apps(R.string.tab_apps, Icons.Outlined.Apps)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalStdlibApi::class
)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    onDownloaderClick: (Int) -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(emptyList())
    val downloaderSources by viewModel.downloaderSources.collectAsStateWithLifecycle(emptyMap())

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
    val currentTab = DownloadsTab.entries[pagerState.currentPage]
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = viewModel::deleteApps,
            title = stringResource(R.string.downloader_delete_apps_title),
            description = stringResource(R.string.downloader_delete_apps_description),
            icon = Icons.Outlined.Delete
        )
    }

    if (showImportDialog) {
        ImportSourceDialog(
            strings = ImportSourceDialogStrings.DOWNLOADERS,
            onDismiss = { showImportDialog = false },
            onLocalSubmit = { uri ->
                showImportDialog = false
                viewModel.createLocalSource(uri)
            },
            onRemoteSubmit = { url, autoUpdate ->
                showImportDialog = false
                viewModel.createRemoteSource(url, autoUpdate)
            }
        )
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.downloads)) },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = onBackClick,
                        tooltip = stringResource(R.string.back),
                    ) { contentDescription ->
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = contentDescription
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (currentTab == DownloadsTab.Apps && viewModel.appSelection.isNotEmpty()) {
                        TooltipIconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            tooltip = stringResource(R.string.delete),
                        ) { contentDescription ->
                            Icon(Icons.Default.Delete, contentDescription)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (pagerState.currentPage != DownloadsTab.Downloaders.ordinal) return@Scaffold

            BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = { showImportDialog = true },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.downloader_add))
                }
            }
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
                            sources = downloaderSources,
                            listState = downloaderListState,
                            onDownloaderClick = onDownloaderClick,
                        )

                        DownloadsTab.Apps -> AppsTabContent(
                            downloadedApps = downloadedApps,
                            listState = appsListState,
                            appSelection = viewModel.appSelection,
                            onToggleApp = viewModel::toggleApp
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadersTabContent(
    sources: Map<Int, Source<DownloaderPackage>>,
    listState: LazyListState,
    onDownloaderClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumnWithScrollbar(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            sources.entries
                .sortedBy { it.key }
                .forEach { (uid, source) ->
                    item(key = uid) {
                        DownloaderItem(
                            source = source,
                            onClick = { onDownloaderClick(uid) }
                        )
                    }
                }
        }
    }
}

@Composable
private fun AppsTabContent(
    downloadedApps: List<DownloadedApp>,
    listState: LazyListState,
    appSelection: Set<DownloadedApp>,
    onToggleApp: (DownloadedApp) -> Unit,
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
                    val selected = app in appSelection
                    ListItem(
                        modifier = Modifier.clickable { onToggleApp(app) },
                        headlineContent = { Text(app.packageName) },
                        supportingContent = { Text(app.version) },
                        leadingContent = (@Composable {
                            HapticCheckbox(
                                checked = selected,
                                onCheckedChange = { onToggleApp(app) }
                            )
                        }).takeIf { appSelection.isNotEmpty() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloaderItem(
    source: Source<DownloaderPackage>,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(source.name, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            val stateText =
                when (source.state) {
                    is State.Available<*> -> null
                    is State.Failed -> R.string.downloader_state_failed
                    is State.Missing -> R.string.downloader_state_missing
                }

            val version = source.loaded?.version
            val relativeTime =
                (source.asRemoteOrNull)?.releasedAt?.relativeTime(LocalContext.current)

            Text(
                text = buildAnnotatedString {
                    append(if (relativeTime != null) "v$version\u2002($relativeTime)" else "v$version")
                    // Error colored state text shown when state isn't available
                    if (stateText != null) withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
                        append("\u2002\u2022\u2002")
                        append(stringResource(stateText))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
