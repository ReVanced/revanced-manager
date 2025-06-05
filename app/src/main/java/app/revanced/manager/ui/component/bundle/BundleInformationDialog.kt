package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.nameState
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.settings.Changelog
import app.revanced.manager.util.relativeTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleInformationDialog(
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    bundle: PatchBundleSource,
    onSearchUpdate: () -> Unit,
    onUpdate: () -> Unit,
    fromUpdateClick: Boolean
) {
    val networkInfo = koinInject<NetworkInfo>()
    val hasNetwork = remember { networkInfo.isConnected() }
    val composableScope = rememberCoroutineScope()
    var viewCurrentBundlePatches by remember { mutableStateOf(false) }
    var viewChangelogDialog by remember { mutableStateOf(false) }
    var updateBundleDialog by remember { mutableStateOf(fromUpdateClick) }
    val isLocal = bundle is LocalPatchBundle
    val state by bundle.state.collectAsStateWithLifecycle()
    val props by remember(bundle) {
        bundle.propsFlow()
    }.collectAsStateWithLifecycle(null)
    val installedProps by remember(bundle) {
        bundle.installedPropsFlow()
    }.collectAsStateWithLifecycle(null)
    val latestProps by remember(bundle) {
        bundle.latestPropsFlow()
    }.collectAsStateWithLifecycle(null)
    val patchCount = remember(state) {
        state.patchBundleOrNull()?.patches?.size ?: 0
    }
    val canUpdateState by remember(bundle) {
        if (bundle is RemotePatchBundle) bundle.canUpdateVersionFlow() else flowOf(false)
    }.collectAsStateWithLifecycle(null)

    FullscreenDialog(
        onDismissRequest = onDismissRequest,
    ) {
        val bundleName by bundle.nameState

        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.patch_bundle_field),
                    onBackClick = onDismissRequest,
                    backIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                    actions = {
                        if (!bundle.isDefault) {
                            IconButton(onClick = onDeleteRequest) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    stringResource(R.string.delete)
                                )
                            }
                        }

                        if (!isLocal) {
                            IconButton(onClick = onSearchUpdate) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    stringResource(R.string.refresh)
                                )
                            }
                        }

                        val canUpdate = bundle is RemotePatchBundle && canUpdateState == true

                        IconButton(
                            onClick = {
                                if (canUpdateState == true) {
                                    updateBundleDialog = true
                                }
                            },
                            enabled = canUpdateState == true
                        ) {
                            if (canUpdateState == true) {
                                BadgedBox(badge = {
                                    Badge(modifier = Modifier.size(6.dp))
                                }) {
                                    Icon(Icons.Outlined.Update, stringResource(R.string.update))
                                }
                            } else {
                                Icon(Icons.Outlined.Update, stringResource(R.string.update))
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            BaseBundleDialog(
                modifier = Modifier.padding(paddingValues),
                isDefault = bundle.isDefault,
                name = bundleName,
                remoteUrl = bundle.asRemoteOrNull?.endpoint,
                patchCount = patchCount,
                version = props?.version,
                autoUpdate = props?.autoUpdate ?: false,
                onAutoUpdateChange = {
                    composableScope.launch {
                        bundle.asRemoteOrNull?.setAutoUpdate(it)
                    }
                },
                searchUpdate = props?.searchUpdate ?: false,
                onSearchUpdateChange = {
                    composableScope.launch {
                        bundle.asRemoteOrNull?.setSearchUpdate(it)
                    }
                },
                onPatchesClick = {
                    viewCurrentBundlePatches = true
                },
                extraFields = {
                    (state as? PatchBundleSource.State.Failed)?.throwable?.let {
                        var showDialog by rememberSaveable {
                            mutableStateOf(false)
                        }
                        if (showDialog) ExceptionViewerDialog(
                            onDismiss = { showDialog = false },
                            text = remember(it) { it.stackTraceToString() }
                        )

                        BundleListItem(
                            headlineText = stringResource(R.string.bundle_error),
                            supportingText = stringResource(R.string.bundle_error_description),
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowRight,
                                    null
                                )
                            },
                            modifier = Modifier.clickable { showDialog = true }
                        )
                    }

                    if (!isLocal) {
                        BundleListItem(
                            headlineText = stringResource(R.string.changelog),
                            supportingText = stringResource(R.string.changelog_description),
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowRight,
                                    null
                                )
                            },
                            modifier = Modifier.clickable { viewChangelogDialog = true }
                        )
                    }

                    if (state is PatchBundleSource.State.Missing && !isLocal) {
                        BundleListItem(
                            headlineText = stringResource(R.string.bundle_error),
                            supportingText = stringResource(R.string.bundle_not_downloaded),
                            modifier = Modifier.clickable(onClick = onUpdate)
                        )
                    }
                }
            )
        }
    }

    if (viewCurrentBundlePatches) {
        BundlePatchesDialog(
            onDismissRequest = {
                viewCurrentBundlePatches = false
            },
            bundle = bundle,
        )
    }

    if (viewChangelogDialog) {
        val publishDate = installedProps?.publishDate
        val changelog = installedProps?.changelog
        val version = props?.version
        FullscreenDialog(
            onDismissRequest = { viewChangelogDialog = false },
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(R.string.changelog),
                        onBackClick = { viewChangelogDialog = false }
                    )
                }
            ) { paddingValues ->
                ColumnWithScrollbar(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (publishDate != null && version != null && changelog != null) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Changelog(
                                markdown = changelog.replace("`", ""),
                                version = version,
                                publishDate = LocalDateTime.parse(publishDate).relativeTime(LocalContext.current)
                            )
                        }
                    }
                }
            }
        }
    }

    if (updateBundleDialog) {
        val publishDate = latestProps?.latestPublishDate
        val changelog = latestProps?.latestChangelog
        val version = latestProps?.latestVersion

        FullscreenDialog(
            onDismissRequest = { updateBundleDialog = false },
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(R.string.update_available),
                        onBackClick = { updateBundleDialog = false }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // Scrollable content
                    ColumnWithScrollbar(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (publishDate != null && version != null && changelog != null) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Changelog(
                                    markdown = changelog.replace("`", ""),
                                    version = version,
                                    publishDate = LocalDateTime
                                        .parse(publishDate)
                                        .relativeTime(LocalContext.current)
                                )
                            }
                        }
                    }
                    if (hasNetwork)
                        HapticExtendedFloatingActionButton(
                            onClick = {
                                onUpdate()
                                updateBundleDialog = false
                                      },
                            icon = { Icon(Icons.Outlined.InstallMobile, null) },
                            text = { Text(stringResource(R.string.download)) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        )
                }
            }
        }
    }
}