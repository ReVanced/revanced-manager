package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NonSuggestedVersionDialog
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.VersionSelectorViewModel
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.simpleMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectorScreen(
    onBackClick: () -> Unit,
    onAppClick: (SelectedApp) -> Unit,
    viewModel: VersionSelectorViewModel
) {
    val supportedVersions by viewModel.supportedVersions.collectAsStateWithLifecycle(emptyMap())
    val downloadedVersions by viewModel.downloadedVersions.collectAsStateWithLifecycle(emptyList())
    val downloadableVersions = viewModel.downloadableApps?.collectAsLazyPagingItems()

    val sortedDownloadedVersions by remember {
        derivedStateOf {
            downloadedVersions
                .distinctBy { it.version }
                .sortedWith(
                    compareByDescending<SelectedApp> { supportedVersions[it.version] }.thenByDescending { it.version }
                )
        }
    }

    if (viewModel.showNonSuggestedVersionDialog)
        NonSuggestedVersionDialog(
            suggestedVersion = viewModel.requiredVersion.orEmpty(),
            onDismiss = viewModel::dismissNonSuggestedVersionDialog
        )

    var showDownloaderSelectionDialog by rememberSaveable {
        mutableStateOf(false)
    }
    if (showDownloaderSelectionDialog) {
        val plugins by viewModel.downloadersFlow.collectAsStateWithLifecycle(emptyList())
        val hasInstalledPlugins by viewModel.hasInstalledPlugins.collectAsStateWithLifecycle(false)

        DownloaderSelectionDialog(
            plugins = plugins,
            hasInstalledPlugins = hasInstalledPlugins,
            onConfirm = {
                viewModel.selectDownloaderPlugin(it)
                showDownloaderSelectionDialog = false
            },
            onDismiss = { showDownloaderSelectionDialog = false }
        )
    }

    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_version),
                actions = {
                    IconButton(onClick = { showDownloaderSelectionDialog = true }) {
                        Icon(Icons.Filled.Download, stringResource(R.string.downloader_select))
                    }
                },
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.select_version)) },
                icon = { Icon(Icons.Default.Check, null) },
                expanded = lazyListState.isScrollingUp,
                onClick = { viewModel.selectedVersion?.let(onAppClick) }
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = lazyListState
        ) {
            viewModel.installedApp?.let { (packageInfo, installedApp) ->
                SelectedApp.Installed(
                    packageName = viewModel.packageName,
                    version = packageInfo.versionName
                ).let {
                    item {
                        SelectedAppItem(
                            selectedApp = it,
                            selected = viewModel.selectedVersion == it,
                            onClick = { viewModel.select(it) },
                            patchCount = supportedVersions[it.version],
                            enabled =
                            !(installedApp?.installType == InstallType.ROOT && !viewModel.rootInstaller.hasRootAccess()),
                            alreadyPatched = installedApp != null && installedApp.installType != InstallType.ROOT
                        )
                    }
                }
            }

            if (sortedDownloadedVersions.isNotEmpty()) item {
                Row(Modifier.fillMaxWidth()) {
                    GroupHeader(stringResource(R.string.downloaded_versions))
                }
            }

            items(
                items = sortedDownloadedVersions,
                key = { it.version }
            ) {
                SelectedAppItem(
                    selectedApp = it,
                    selected = viewModel.selectedVersion == it,
                    onClick = { viewModel.select(it) },
                    patchCount = supportedVersions[it.version]
                )
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    GroupHeader(stringResource(R.string.downloadable_versions))
                }
            }
            if (downloadableVersions == null) {
                item {
                    Text(stringResource(R.string.downloader_not_selected))
                }
            } else {
                (downloadableVersions.loadState.prepend as? LoadState.Error)?.let { errorState ->
                    item {
                        errorState.Render()
                    }
                }

                items(
                    count = downloadableVersions.itemCount,
                    key = downloadableVersions.itemKey { it.version }
                ) {
                    val item = downloadableVersions[it]!!

                    SelectedAppItem(
                        selectedApp = item,
                        selected = viewModel.selectedVersion == item,
                        onClick = { viewModel.select(item) },
                        patchCount = supportedVersions[item.version]
                    )
                }

                val loadStates = arrayOf(
                    downloadableVersions.loadState.append,
                    downloadableVersions.loadState.refresh
                )

                if (loadStates.any { it is LoadState.Loading }) {
                    item {
                        LoadingIndicator()
                    }
                } else if (downloadableVersions.itemCount == 0) {
                    item { Text(stringResource(R.string.downloader_no_versions)) }
                }

                loadStates.firstNotNullOfOrNull { it as? LoadState.Error }?.let { errorState ->
                    item {
                        errorState.Render()
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedAppItem(
    selectedApp: SelectedApp,
    selected: Boolean,
    onClick: () -> Unit,
    patchCount: Int?,
    enabled: Boolean = true,
    alreadyPatched: Boolean = false,
) {
    ListItem(
        leadingContent = { RadioButton(selected, null) },
        headlineContent = { Text(selectedApp.version) },
        supportingContent = when (selectedApp) {
            is SelectedApp.Installed ->
                if (alreadyPatched) {
                    { Text(stringResource(R.string.already_patched)) }
                } else {
                    { Text(stringResource(R.string.installed)) }
                }

            is SelectedApp.Local -> {
                { Text(stringResource(R.string.already_downloaded)) }
            }

            else -> null
        },
        trailingContent = patchCount?.let {
            {
                Text(pluralStringResource(R.plurals.patch_count, it, it))
            }
        },
        modifier = Modifier
            .clickable(enabled = !alreadyPatched && enabled, onClick = onClick)
            .run {
                if (!enabled || alreadyPatched) alpha(0.5f)
                else this
            }
    )
}

@Composable
private fun LoadState.Error.Render() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message =
            remember(error) { error.simpleMessage().orEmpty() }
        Text(stringResource(R.string.error_occurred))
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 15.dp)
        )
        Text(error.stackTraceToString())
    }
}

@Composable
private fun DownloaderSelectionDialog(
    plugins: List<LoadedDownloaderPlugin>,
    hasInstalledPlugins: Boolean,
    onConfirm: (LoadedDownloaderPlugin) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPackageName: String? by rememberSaveable {
        mutableStateOf(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = selectedPackageName != null,
                onClick = { onConfirm(plugins.single { it.packageName == selectedPackageName }) }
            ) {
                Text(stringResource(R.string.select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.downloader_select))
        },
        icon = {
            Icon(Icons.Filled.Download, null)
        },
        // TODO: fix dialog header centering issue
        // textHorizontalPadding = PaddingValues(horizontal = if (plugins.isNotEmpty()) 0.dp else 24.dp),
        text = {
            LazyColumn {
                items(plugins, key = { it.packageName }) {
                    ListItem(
                        modifier = Modifier.clickable { selectedPackageName = it.packageName },
                        headlineContent = { Text(it.name) },
                        leadingContent = {
                            RadioButton(
                                selected = selectedPackageName == it.packageName,
                                onClick = { selectedPackageName = it.packageName }
                            )
                        }
                    )
                }

                if (plugins.isEmpty()) {
                    item {
                        val resource =
                            if (hasInstalledPlugins) R.string.downloader_no_plugins_available else R.string.downloader_no_plugins_installed

                        Text(stringResource(resource))
                    }
                }
            }
        }
    )
}