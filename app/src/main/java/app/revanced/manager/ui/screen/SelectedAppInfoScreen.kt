package app.revanced.manager.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAppInfoScreen(
    onPatchSelectorClick: (packageName: String, version: String?, PatchSelection?, Options) -> Unit,
    onRequiredOptions: (packageName: String, version: String?, PatchSelection?, Options) -> Unit,
    onPatchClick: () -> Unit,
    onVersionClick: (packageName: String, patchSelection: PatchSelection, selectedVersion: SelectedVersion) -> Unit,
    onSourceClick: (packageName: String, version: String?) -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = LocalContext.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkConnected = remember { networkInfo.isConnected() }
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.packageName
    val composableScope = rememberCoroutineScope()

    val error by vm.errorFlow.collectAsStateWithLifecycle(null)

    val selectedVersion by vm.selectedVersion.collectAsStateWithLifecycle()
    val resolvedVersion by vm.resolvedVersion.collectAsStateWithLifecycle(null)

    val selectedSource by vm.selectedSource.collectAsStateWithLifecycle()
    val resolvedSource by vm.resolvedSource.collectAsStateWithLifecycle(null)

    val customSelection by vm.customSelection.collectAsStateWithLifecycle(null)
    val fullPatchSelection by vm.patchSelection.collectAsStateWithLifecycle(emptyMap())
    val patchCount = fullPatchSelection.patchCount

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val plugins by vm.plugins.collectAsStateWithLifecycle(emptyList())

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
                        if (!vm.hasSetRequiredOptions(fullPatchSelection)) {
                            onRequiredOptions(
                                vm.packageName,
                                resolvedVersion,
                                customSelection,
                                vm.options,
                            )
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
                vm.selectedAppInfo?.let {
                    Text(
                        it.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PageItem(
                R.string.patch_selector_item,
                stringResource(R.string.patch_selector_item_description, patchCount),
                onClick = {
                    onPatchSelectorClick(
                        vm.packageName,
                        resolvedVersion,
                        customSelection,
                        vm.options
                    )
                }
            )

            val versionText = resolvedVersion ?: "Any available version"
            val versionDescription = if (selectedVersion is SelectedVersion.Auto)
                "Auto ($versionText)" // stringResource(R.string.selected_app_meta_auto_version, actualVersion)
            else versionText

            PageItem(
                R.string.version_selector_item,
                versionDescription,
                onClick = {
                    onVersionClick(
                        packageName,
                        fullPatchSelection,
                        selectedVersion,
                    )
                },
            )

            PageItem(
                R.string.apk_source_selector_item,
                when (selectedSource) {
                    else -> "Sourcing the source"
                },
                onClick = { onSourceClick(packageName, versionText) },
            )

            error?.let {
                Text(
                    stringResource(it.resourceId),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val needsInternet = resolvedSource is SelectedSource.Plugin

                when {
                    !needsInternet -> {}
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

@Composable
private fun PageItem(
    @StringRes title: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
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
            Text(
                description,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
        }
    )
}