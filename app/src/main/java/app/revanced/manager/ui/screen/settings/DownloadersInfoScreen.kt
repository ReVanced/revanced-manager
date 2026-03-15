package app.revanced.manager.ui.screen.settings

import android.webkit.URLUtil.isValidUrl
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.haptics.HapticSwitch
import app.revanced.manager.ui.component.settings.SafeguardBooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import kotlinx.coroutines.launch
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
    val source = downloaderStates[uid] ?: return
    val remote = source.asRemoteOrNull


    val appName = source.name
    val displayNames = remember(source) {
        source.loaded?.downloaders?.map { it.name }.orEmpty()
    }
    val version = source.loaded?.version.orEmpty()
    val isDeleting = viewModel.deletingDownloaderUid == uid

    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    val coroutineScope = rememberCoroutineScope()

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                coroutineScope.launch {
                    viewModel.deleteDownloader(source).join()
                    onBackClick()
                }
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
                subtitle = version.takeIf { it.isNotEmpty() }?.let {
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
                    if (!source.isDefault) IconButton(
                        onClick = { showDeleteConfirmationDialog = true },
                        enabled = !isDeleting,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }

                    remote?.let {
                        val hasNetwork = remember { viewModel.networkInfo.isConnected() }
                        if (!hasNetwork) return@let

                        IconButton(
                            onClick = { viewModel.updateDownloader(it) },
                            enabled = !isDeleting,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(Icons.Filled.Update, stringResource(R.string.update))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        )
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = scrollState,
        ) {
            ListSection(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                remote?.let { remoteSource ->
                    val autoUpdate = remoteSource.autoUpdate
                    SettingsListItem(
                        headlineContent = stringResource(R.string.auto_update),
                        supportingContent = stringResource(R.string.auto_update_description),
                        trailingContent = {
                            HapticSwitch(
                                checked = autoUpdate,
                                onCheckedChange = { viewModel.setAutoUpdate(remoteSource, it) },
                                thumbContent = if (autoUpdate) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        },
                        onClick = { viewModel.setAutoUpdate(remoteSource, !autoUpdate) }
                    )
                }

                if (source.isDefault) {
                    SafeguardBooleanItem(
                        preference = viewModel.usePrereleases,
                        headline = R.string.downloader_prereleases,
                        description = stringResource(
                            R.string.downloader_prereleases_description,
                            source.name
                        ),
                        dialogTitle = R.string.prerelease_title,
                        confirmationText = R.string.prereleases_warning,
                        onValueChange = viewModel::updateUsePrereleases
                    )
                }

                remote?.endpoint?.takeUnless { source.isDefault }?.let { url ->
                    var showUrlInputDialog by rememberSaveable { mutableStateOf(false) }

                    if (showUrlInputDialog) {
                        TextInputDialog(
                            initial = url,
                            title = stringResource(R.string.downloader_url),
                            onDismissRequest = { showUrlInputDialog = false },
                            onConfirm = {
                                showUrlInputDialog = false
                                // TODO: Not implemented
                            },
                            validator = {
                                if (it.isEmpty()) return@TextInputDialog false
                                isValidUrl(it)
                            }
                        )
                    }

                    SettingsListItem(
                        headlineContent = stringResource(R.string.downloader_url),
                        supportingContent = url.ifEmpty {
                            stringResource(R.string.field_not_set)
                        },
                        onClick = null
                    )
                }
            }

            if (displayNames.isNotEmpty()) {
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
}