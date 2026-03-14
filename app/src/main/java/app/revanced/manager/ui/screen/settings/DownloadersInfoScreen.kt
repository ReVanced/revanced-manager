package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.sources.Source.State
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalStdlibApi::class
)
@Composable
fun DownloaderInfoScreen(
    uid: Int,
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloaderStates by viewModel.downloaderSources.collectAsStateWithLifecycle(emptyMap())
    val source = downloaderStates[uid]

    val appName = source?.name.orEmpty()
    val displayNames = remember(source) {
        source?.loaded?.downloaders?.map { it.name }.orEmpty()
    }
    val isEnabled = source?.let { it.state !is State.Missing } ?: true
    val versionName = "TODO" // packageInfo.versionName.orEmpty()
    val isDeleting = viewModel.deletingDownloaderUid == uid

    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                source?.let(viewModel::deleteDownloader)
            },
            title = stringResource(R.string.delete),
            description = stringResource(R.string.downloader_delete_single_description, appName),
            icon = Icons.Outlined.Delete
        )
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(appName) },
                subtitle = versionName.takeIf { it.isNotEmpty() }?.let {
                    { Text("v$it") }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmationDialog = true },
                        enabled = !isDeleting,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }
                    IconButton(
                        onClick = { source?.asRemoteOrNull?.let(viewModel::updateDownloader) },
                        enabled = !isDeleting,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(Icons.Filled.Update, stringResource(R.string.update))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                if (isDeleting) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.api_downloader_deleting))
                    }
                } /*else if (isEnabled) {
                    FilledTonalButton(
                        onClick = { viewModel.revokeDownloaderTrust(packageName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.disable))
                    }
                } else {
                    Button(
                        onClick = { showTrustDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.enable))
                    }
                }*/
            }
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        )
    ) { paddingValues ->
        if (displayNames.isNotEmpty()) {
            ColumnWithScrollbar(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = scrollState,
            ) {
                ListSection(
                    title = stringResource(R.string.downloaders),
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    displayNames.forEachIndexed { index, downloaderName ->
                        SegmentedListItem(
                            onClick = {},
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = displayNames.size
                            )
                        ) {
                            Text(downloaderName)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    icon = Icons.Outlined.SignalWifiOff,
                    title = R.string.downloader_sources_unavailable_title,
                    description = R.string.downloader_sources_unavailable_description
                )
            }
        }
    }
}