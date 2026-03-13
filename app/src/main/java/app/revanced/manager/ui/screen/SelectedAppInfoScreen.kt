package app.revanced.manager.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.enabled
import app.revanced.manager.util.patchCount
import app.revanced.manager.util.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAppInfoScreen(
    onPatchSelectorClick: (packageName: String, version: String?, PatchSelection?, Options) -> Unit,
    onRequiredOptions: (packageName: String, version: String?, PatchSelection?, Options) -> Unit,
    onPatchClick: () -> Unit,
    onVersionClick: (packageName: String, patchSelection: PatchSelection, selectedVersion: SelectedVersion, localPath: String?) -> Unit,
    onSourceClick: (packageName: String, version: String?, SelectedSource, localPath: String?) -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkConnected = remember { networkInfo.isConnected() }
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.packageName
    val composableScope = rememberCoroutineScope()

    val downloaders by vm.downloaders.collectAsStateWithLifecycle(emptyList())
    val error by vm.errorFlow.collectAsStateWithLifecycle(null)

    val selectedVersion by vm.selectedVersion.collectAsStateWithLifecycle()
    val resolvedVersion by vm.resolvedVersion.collectAsStateWithLifecycle(null)
    val versionSelection by vm.versionPatchSelection.collectAsStateWithLifecycle(emptyMap())

    val selectedSource by vm.selectedSource.collectAsStateWithLifecycle()
    val resolvedSource by vm.resolvedSource.collectAsStateWithLifecycle(SelectedSource.Downloader())

    val customSelection by vm.customSelection.collectAsStateWithLifecycle(null)
    val currentPatchSelection by vm.patchSelection.collectAsStateWithLifecycle(emptyMap())
    val patchCount = currentPatchSelection.patchCount

    val incompatibleCount by vm.incompatiblePatchCount.collectAsStateWithLifecycle(0)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_info),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            if (error != null) return@Scaffold

            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        stringResource(R.string.patch)
                    )
                },
                onClick = patchClick@{
                    if (patchCount == 0) {
                        context.toast(context.getString(R.string.no_patches_selected))
                        return@patchClick
                    }

                    composableScope.launch {
                        if (!vm.hasSetRequiredOptions(currentPatchSelection)) {
                            onRequiredOptions(
                                packageName,
                                resolvedVersion,
                                customSelection,
                                vm.options,
                            )
                            return@launch
                        }

                        vm.reloadDownloaders()
                        if (vm.errorFlow.first() != null) {
                            context.toast(context.getString(R.string.no_downloader_available))
                            return@launch
                        }

                        onPatchClick()
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppInfo(vm.selectedAppInfo, placeholderLabel = packageName) {
                vm.selectedAppInfo?.packageName?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PageItem(
                title = R.string.patch_selector_item,
                description = stringResource(R.string.patch_selector_item_description, patchCount),
                onClick = {
                    onPatchSelectorClick(
                        packageName,
                        resolvedVersion,
                        customSelection,
                        vm.options
                    )
                },
                extraDescription = if (incompatibleCount > 0) {
                    {
                        Text(
                            pluralStringResource(
                                R.plurals.version_selector_incompatible_patches,
                                incompatibleCount,
                                incompatibleCount
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    null
                }
            )

            PageItem(
                title = R.string.version_selector_item,
                description = selectedVersionDescription(selectedVersion, resolvedVersion),
                onClick = {
                    onVersionClick(
                        packageName,
                        versionSelection,
                        selectedVersion,
                        vm.localPath,
                    )
                },
            )

            PageItem(
                title = R.string.apk_source_selector_item,
                description = selectedSourceDescription(
                    selectedSource = selectedSource,
                    resolvedSource = resolvedSource,
                    downloaders = downloaders,
                ),
                onClick = {
                    onSourceClick(
                        packageName,
                        resolvedVersion,
                        selectedSource,
                        vm.localPath,
                    )
                },
            )

            error?.let {
                Text(
                    stringResource(it.resourceId),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (resolvedSource is SelectedSource.Downloader) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        !networkConnected -> {
                            NotificationCard(
                                isWarning = true,
                                icon = Icons.Outlined.WarningAmber,
                                text = stringResource(R.string.network_unavailable_warning),
                                onDismiss = null
                            )
                        }

                        networkMetered -> {
                            NotificationCard(
                                isWarning = true,
                                icon = Icons.Outlined.WarningAmber,
                                text = stringResource(R.string.network_metered_warning),
                                onDismiss = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun selectedVersionDescription(
    selectedVersion: SelectedVersion,
    resolvedVersion: String?,
): String {
    val resolvedText = resolvedVersion ?: stringResource(R.string.selected_app_meta_any_version)
    return if (selectedVersion is SelectedVersion.Auto) {
        stringResource(R.string.selected_app_meta_auto, stringResource(R.string.app_source_dialog_option_auto), resolvedText)
    } else {
        resolvedText
    }
}

@Composable
private fun selectedSourceDescription(
    selectedSource: SelectedSource,
    resolvedSource: SelectedSource,
    downloaders: List<LoadedDownloader>,
): String {
    val resolvedText = when (resolvedSource) {
        SelectedSource.Auto -> stringResource(R.string.app_source_dialog_option_auto)
        SelectedSource.Installed -> stringResource(R.string.selected_app_meta_source_installed_apk)
        is SelectedSource.Downloaded -> stringResource(R.string.selected_app_meta_source_downloaded_apk)
        is SelectedSource.Local -> stringResource(R.string.selected_app_meta_source_local_apk)
        is SelectedSource.Downloader -> {
            if (resolvedSource.packageName == null && resolvedSource.className == null) {
                stringResource(R.string.selected_app_meta_source_any_downloader)
            } else {
                downloaders.firstOrNull { downloader ->
                    downloader.packageName == resolvedSource.packageName &&
                        downloader.className == resolvedSource.className
                }?.name ?: stringResource(R.string.selected_app_meta_source_any_downloader)
            }
        }
    }

    return if (selectedSource is SelectedSource.Auto) {
        stringResource(
            R.string.selected_app_meta_auto,
            stringResource(R.string.app_source_dialog_option_auto),
            resolvedText
        )
    } else {
        resolvedText
    }
}

@Composable
private fun PageItem(
    @StringRes title: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    extraDescription: @Composable (ColumnScope.() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled, onClick = onClick)
            .enabled(enabled),
        headlineContent = {
            Text(
                stringResource(title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        supportingContent = {
            Column {
                Text(
                    description,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium
                )
                extraDescription?.invoke(this)
            }
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
        }
    )
}