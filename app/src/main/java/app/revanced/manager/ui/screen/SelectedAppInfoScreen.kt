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
import androidx.compose.runtime.derivedStateOf
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
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.enabled
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedAppInfoScreen(
    onPatchSelectorClick: (SelectedApp, PatchSelection?, Options) -> Unit,
    onRequiredOptions: (SelectedApp, PatchSelection?, Options) -> Unit,
    onPatchClick: () -> Unit,
    onVersionClick: (packageName: String, patchSelection: PatchSelection, currentSelection: SelectedVersion) -> Unit,
    onSourceClick: (packageName: String, version: String?) -> Unit,
    onBackClick: () -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = LocalContext.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkConnected = remember { networkInfo.isConnected() }
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by vm.bundleInfoFlow.collectAsStateWithLifecycle(emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val patches by remember {
        derivedStateOf {
            vm.getPatches(bundles, allowIncompatiblePatches)
        }
    }
    val selectedPatchCount = patches.values.sumOf { it.size }

//    val patches2 by remember {
//        derivedStateOf {
//            vm.patchSelection(bundles, allowIncompatiblePatches).collectAsStateWithLifecycle()
//        }
//    }

    val composableScope = rememberCoroutineScope()

    val error by vm.errorFlow.collectAsStateWithLifecycle(null)

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
                    if (selectedPatchCount == 0) {
                        context.toast(context.getString(R.string.no_patches_selected))

                        return@patchClick
                    }

                    composableScope.launch {
                        if (!vm.hasSetRequiredOptions(patches)) {
                            onRequiredOptions(
                                vm.selectedApp,
                                vm.getCustomPatches(bundles, allowIncompatiblePatches),
                                vm.options
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
        val plugins by vm.plugins.collectAsStateWithLifecycle(emptyList())

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
                stringResource(
                    R.string.patch_selector_item_description,
                    selectedPatchCount
                ) + "\n⚠\uFE0F 3 incompatible",
                onClick = {
                    onPatchSelectorClick(
                        vm.selectedApp,
                        vm.getCustomPatches(
                            bundles,
                            allowIncompatiblePatches
                        ),
                        vm.options
                    )
                }
            )

            if (vm.selectedApp !is SelectedApp.Local || !(vm.selectedApp as SelectedApp.Local).temporary) {
                val selectedVersion by vm.selectedVersion.collectAsStateWithLifecycle(SelectedVersion.Auto)
                val resolvedVersion by vm.resolvedVersion.collectAsStateWithLifecycle(null)

                val version = resolvedVersion ?: "Any available version"

                val description = if (selectedVersion is SelectedVersion.Auto)
                    "Auto ($version)" // stringResource(R.string.selected_app_meta_auto_version, actualVersion)
                    else version


                PageItem(
                    R.string.version_selector_item,
                    description,
//                    "Auto (${requiredVersion ?: stringResource(R.string.selected_app_meta_any_version)})", // ⚠️ 1 Patch incompatible
                    onClick = { onVersionClick(packageName, patches, selectedVersion) },
                )
            }

            PageItem(
                R.string.apk_source_selector_item,
                when (val app = vm.selectedApp) {
                    is SelectedApp.Search -> "Auto (Downloaded APK)" // stringResource(R.string.apk_source_auto)
                    is SelectedApp.Installed -> app.version + " (Installed)" // stringResource(R.string.apk_source_installed)
                    is SelectedApp.Download -> plugins.find { it.packageName == app.data.pluginPackageName }?.name ?: app.data.pluginPackageName
                    is SelectedApp.Local -> if (app.temporary) "${app.version} (Local File)" else "Downloaded APK"


//                        stringResource(
//                        R.string.apk_source_downloader,
//                        plugins.find { it.packageName == app.data.pluginPackageName }?.name
//                            ?: app.data.pluginPackageName
//                    )
                    // stringResource(R.string.apk_source_local)
                },
                onClick = { onSourceClick(packageName, version) },
                enabled = !vm.selectedApp.let { it is SelectedApp.Local && it.temporary }, // Disable for APK from storage
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
                val needsInternet =
                    vm.selectedApp.let { it is SelectedApp.Search || it is SelectedApp.Download }

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
private fun PageItem(@StringRes title: Int, description: String, onClick: () -> Unit, enabled: Boolean = true) {
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