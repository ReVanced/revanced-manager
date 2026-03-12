package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.AutoUpdatesDialog
import app.revanced.manager.ui.component.AvailableUpdateDialog
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.bundle.BundleInformationDialog
import app.revanced.manager.ui.component.bundle.BundleTopBar
import app.revanced.manager.ui.component.bundle.ImportPatchBundleDialog
import app.revanced.manager.ui.component.haptics.HapticFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTab
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.navigation.SelectedApplicationInfo
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

enum class DashboardPage(
    val titleResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(R.string.tab_apps, Icons.Outlined.Apps),
    BUNDLES(R.string.tab_patches, Icons.Outlined.Source),
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
    onAnnouncementClick: (ReVancedAnnouncement) -> Unit,
    onDownloaderClick: () -> Unit,
    onAppClick: (String) -> Unit,
    onPatchableAppClick: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
) {
    val sources by vm.sources.collectAsStateWithLifecycle(emptyList())
    val patchCounts by vm.patchCounts.collectAsStateWithLifecycle(emptyMap())
    val showNewDownloaderNotification by vm.newDownloadersAvailable.collectAsStateWithLifecycle(false)

    val androidContext = LocalContext.current
    val composableScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = DashboardPage.DASHBOARD.ordinal,
        initialPageOffsetFraction = 0f
    ) { DashboardPage.entries.size }

    var sourceEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedSourceUids by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val selectedSourceCount by remember { derivedStateOf { selectedSourceUids.size } }

    val readOnlyPatchesVm: PatchesSelectorViewModel = koinViewModel(
        key = "dashboard-patches-selector",
        parameters = {
            parametersOf(
                SelectedApplicationInfo.PatchesSelector.ViewModelParams(
                    app = SelectedApp.Search(packageName = "", version = null),
                    currentSelection = null,
                    options = emptyMap(),
                    readOnly = true,
                )
            )
        }
    )

    val firstLaunch by vm.prefs.firstLaunch.getAsState()
    if (firstLaunch) AutoUpdatesDialog(vm::applyAutoUpdatePrefs)

    var showAddBundleDialog by rememberSaveable { mutableStateOf(false) }
    if (showAddBundleDialog) {
        ImportPatchBundleDialog(
            onDismiss = { showAddBundleDialog = false },
            onLocalSubmit = { patches ->
                showAddBundleDialog = false
                vm.createLocalSource(patches)
            },
            onRemoteSubmit = { url, autoUpdate ->
                showAddBundleDialog = false
                vm.createRemoteSource(url, autoUpdate)
            }
        )
    }

    var showUpdateDialog by rememberSaveable { mutableStateOf(true) }
    val showManagerUpdateDialogOnLaunch by vm.prefs.showManagerUpdateDialogOnLaunch.getAsState()
    val availableUpdate = vm.updatedManagerVersion
    if (showUpdateDialog && showManagerUpdateDialogOnLaunch && availableUpdate != null) {
        AvailableUpdateDialog(
            onDismiss = { showUpdateDialog = false },
            setShowManagerUpdateDialogOnLaunch = vm::setShowManagerUpdateDialogOnLaunch,
            onConfirm = onUpdateClick,
            newVersion = availableUpdate
        )
    }

    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                vm.deleteSources(selectedSourceUids)
                selectedSourceUids = emptySet()
                sourceEditMode = false
            },
            title = stringResource(R.string.delete),
            description = stringResource(R.string.patches_delete_multiple_dialog_description),
            icon = Icons.Outlined.Delete
        )
    }

    var aboutSourceUid by rememberSaveable { mutableStateOf<Int?>(null) }
    aboutSourceUid?.let { uid ->
        val src = sources.firstOrNull { it.uid == uid }
        if (src != null) {
            BundleInformationDialog(
                src = src,
                patchCount = patchCounts[src.uid] ?: 0,
                onDismissRequest = { aboutSourceUid = null },
                onDeleteRequest = {
                    vm.deleteSources(setOf(src.uid))
                    aboutSourceUid = null
                },
                onUpdate = { vm.updateSource(src.uid) },
            )
        }
    }

    Scaffold(
        topBar = {
            val onPatchesTab = pagerState.currentPage == DashboardPage.BUNDLES.ordinal

            if (onPatchesTab && sourceEditMode) {
                BundleTopBar(
                    title = stringResource(R.string.patches_selected, selectedSourceCount),
                    onBackClick = {
                        sourceEditMode = false
                        selectedSourceUids = emptySet()
                    },
                    backIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            enabled = selectedSourceUids.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete))
                        }
                    }
                )
            } else {
                AppTopBar(
                    title = stringResource(R.string.app_name),
                    actions = {
                        if (!vm.updatedManagerVersion.isNullOrEmpty()) {
                            IconButton(onClick = onUpdateClick) {
                                BadgedBox(badge = { Badge(modifier = Modifier.size(6.dp)) }) {
                                    Icon(Icons.Outlined.Update, stringResource(R.string.update))
                                }
                            }
                        }

                        IconButton(onClick = onAnnouncementsClick) {
                            BadgedBox(
                                badge = {
                                    if (vm.unreadAnnouncement != null) {
                                        Badge(modifier = Modifier.size(6.dp))
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.Notifications, stringResource(R.string.announcements))
                            }
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                        }
                    },
                    applyContainerColor = true
                )
            }
        },
        floatingActionButton = {
            val onPatchesTab = pagerState.currentPage == DashboardPage.BUNDLES.ordinal
            if (!onPatchesTab) return@Scaffold

            HapticFloatingActionButton(
                onClick = {
                    if (sourceEditMode) {
                        showAddBundleDialog = true
                    } else {
                        sourceEditMode = true
                        selectedSourceUids = emptySet()
                    }
                }
            ) {
                val icon = if (!sourceEditMode) Icons.Outlined.Edit else Icons.Default.Add
                Icon(icon, stringResource(if (!sourceEditMode) R.string.edit else R.string.add))
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                DashboardPage.entries.forEachIndexed { index, page ->
                    HapticTab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            composableScope.launch { pagerState.animateScrollToPage(index) }
                            if (index != DashboardPage.BUNDLES.ordinal) {
                                sourceEditMode = false
                                selectedSourceUids = emptySet()
                            }
                        },
                        text = { Text(stringResource(page.titleResId)) },
                        icon = { Icon(page.icon, null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Notifications(
                if (!Aapt.supportsDevice()) {
                    {
                        NotificationCard(
                            isWarning = true,
                            icon = Icons.Outlined.WarningAmber,
                            text = stringResource(R.string.unsupported_architecture_warning),
                            onDismiss = null
                        )
                    }
                } else null,
                if (vm.showBatteryOptimizationsWarning) {
                    {
                        val batteryOptimizationsLauncher =
                            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                vm.updateBatteryOptimizationsWarning()
                            }
                        NotificationCard(
                            isWarning = true,
                            icon = Icons.Default.BatteryAlert,
                            text = stringResource(R.string.battery_optimization_notification),
                            onClick = {
                                batteryOptimizationsLauncher.launch(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.fromParts("package", androidContext.packageName, null)
                                    )
                                )
                            }
                        )
                    }
                } else null,
                if (showNewDownloaderNotification) {
                    {
                        NotificationCard(
                            text = stringResource(R.string.new_downloader_notification),
                            icon = Icons.Outlined.Download,
                            modifier = Modifier.clickable(onClick = onDownloaderClick),
                            actions = {
                                TextButton(onClick = vm::ignoreNewDownloaders) {
                                    Text(stringResource(R.string.dismiss))
                                }
                            }
                        )
                    }
                } else null,
                vm.unreadAnnouncement?.let { announcement ->
                    {
                        NotificationCard(
                            text = stringResource(R.string.new_announcement, announcement.title),
                            icon = Icons.Filled.Notifications,
                            actions = {
                                TextButton(onClick = vm::markUnreadAnnouncementRead) {
                                    Text(stringResource(R.string.dismiss))
                                }
                                TextButton(
                                    onClick = {
                                        vm.markUnreadAnnouncementRead()
                                        onAnnouncementClick(announcement)
                                    }
                                ) {
                                    Text(stringResource(R.string.view_announcement))
                                }
                            },
                            isWarning = announcement.level > 0
                        )
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize(),
                pageContent = { index ->
                    when (DashboardPage.entries[index]) {
                        DashboardPage.DASHBOARD -> InstalledAppsScreen(
                            onAppClick = { onAppClick(it.currentPackageName) },
                            onPatchableAppClick = onPatchableAppClick,
                            onStorageSelect = onStorageSelect,
                        )

                        DashboardPage.BUNDLES -> {
                            BackHandler {
                                if (sourceEditMode) {
                                    sourceEditMode = false
                                    selectedSourceUids = emptySet()
                                } else composableScope.launch {
                                    pagerState.animateScrollToPage(DashboardPage.DASHBOARD.ordinal)
                                }
                            }

                            PatchesSelectorScreen(
                                onSave = { _, _ -> },
                                onBackClick = {
                                    if (sourceEditMode) {
                                        sourceEditMode = false
                                        selectedSourceUids = emptySet()
                                    } else composableScope.launch {
                                        pagerState.animateScrollToPage(DashboardPage.DASHBOARD.ordinal)
                                    }
                                },
                                viewModel = readOnlyPatchesVm,
                                sourceEditMode = sourceEditMode,
                                selectedSourceUids = selectedSourceUids,
                                onToggleSourceSelection = { uid ->
                                    selectedSourceUids = if (uid in selectedSourceUids) {
                                        selectedSourceUids - uid
                                    } else {
                                        selectedSourceUids + uid
                                    }
                                },
                                onSourceMenuAction = { uid, action ->
                                    when (action) {
                                        SourceMenuAction.MORE -> aboutSourceUid = uid
                                        SourceMenuAction.REFRESH -> vm.updateSource(uid)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun Notifications(
    vararg notifications: (@Composable () -> Unit)?,
) {
    val activeNotifications = notifications.filterNotNull()

    if (activeNotifications.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            activeNotifications.forEach { notification ->
                notification()
            }
        }
    }
}

@Composable
fun Android11Dialog(onDismissRequest: () -> Unit, onContinue: () -> Unit) {
    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.continue_))
            }
        },
        title = {
            Text(stringResource(R.string.android_11_bug_dialog_title))
        },
        icon = {
            Icon(Icons.Outlined.BugReport, null)
        },
        text = {
            Text(stringResource(R.string.android_11_bug_dialog_description))
        }
    )
}