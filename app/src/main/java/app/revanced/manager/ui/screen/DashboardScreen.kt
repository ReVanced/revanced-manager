package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.AutoUpdatesDialog
import app.revanced.manager.ui.component.AvailableUpdateDialog
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.bundle.BundleTopBar
import app.revanced.manager.ui.component.haptics.HapticFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTab
import app.revanced.manager.ui.component.bundle.ImportPatchBundleDialog
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.util.RequestInstallAppsContract
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

enum class DashboardPage(
    val titleResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(R.string.tab_apps, Icons.Outlined.Apps),
    BUNDLES(R.string.tab_bundles, Icons.Outlined.Source),
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    onAppSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onDownloaderPluginClick: () -> Unit,
    onAppClick: (InstalledApp) -> Unit
) {
    val bundlesSelectable by remember { derivedStateOf { vm.selectedSources.size > 0 } }
    val availablePatches by vm.availablePatches.collectAsStateWithLifecycle(0)
    val showNewDownloaderPluginsNotification by vm.newDownloaderPluginsAvailable.collectAsStateWithLifecycle(
        false
    )
    val androidContext = LocalContext.current
    val composableScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = DashboardPage.DASHBOARD.ordinal,
        initialPageOffsetFraction = 0f
    ) { DashboardPage.entries.size }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != DashboardPage.BUNDLES.ordinal) vm.cancelSourceSelection()
    }

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

    var showUpdateDialog by rememberSaveable { mutableStateOf(vm.prefs.showManagerUpdateDialogOnLaunch.getBlocking()) }
    val availableUpdate by remember {
        derivedStateOf { vm.updatedManagerVersion.takeIf { showUpdateDialog } }
    }

    availableUpdate?.let { version ->
        AvailableUpdateDialog(
            onDismiss = { showUpdateDialog = false },
            setShowManagerUpdateDialogOnLaunch = vm::setShowManagerUpdateDialogOnLaunch,
            onConfirm = onUpdateClick,
            newVersion = version
        )
    }

    val context = LocalContext.current
    var showAndroid11Dialog by rememberSaveable { mutableStateOf(false) }
    val installAppsPermissionLauncher =
        rememberLauncherForActivityResult(RequestInstallAppsContract) { granted ->
            showAndroid11Dialog = false
            if (granted) onAppSelectorClick()
        }
    if (showAndroid11Dialog) Android11Dialog(
        onDismissRequest = {
            showAndroid11Dialog = false
        },
        onContinue = {
            installAppsPermissionLauncher.launch(context.packageName)
        }
    )

    Scaffold(
        topBar = {
            if (bundlesSelectable) {
                BundleTopBar(
                    title = stringResource(R.string.bundles_selected, vm.selectedSources.size),
                    onBackClick = vm::cancelSourceSelection,
                    backIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                vm.selectedSources.forEach { if (!it.isDefault) vm.delete(it) }
                                vm.cancelSourceSelection()
                            }
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                stringResource(R.string.delete)
                            )
                        }
                        IconButton(
                            onClick = {
                                vm.selectedSources.forEach { vm.update(it) }
                                vm.cancelSourceSelection()
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                stringResource(R.string.refresh)
                            )
                        }
                    }
                )
            } else {
                AppTopBar(
                    title = stringResource(R.string.app_name),
                    actions = {
                        if (!vm.updatedManagerVersion.isNullOrEmpty()) {
                            IconButton(
                                onClick = onUpdateClick,
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            // A size value above 6.dp forces the Badge icon to be closer to the center, fixing a clipping issue
                                            modifier = Modifier.size(7.dp),
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                ) {
                                    Icon(Icons.Outlined.Update, stringResource(R.string.update))
                                }
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            HapticFloatingActionButton(
                onClick = {
                    vm.cancelSourceSelection()

                    when (pagerState.currentPage) {
                        DashboardPage.DASHBOARD.ordinal -> {
                            if (availablePatches < 1) {
                                androidContext.toast(androidContext.getString(R.string.patches_unavailable))
                                composableScope.launch {
                                    pagerState.animateScrollToPage(
                                        DashboardPage.BUNDLES.ordinal
                                    )
                                }
                                return@HapticFloatingActionButton
                            }
                            if (vm.android11BugActive) {
                                showAndroid11Dialog = true
                                return@HapticFloatingActionButton
                            }

                            onAppSelectorClick()
                        }

                        DashboardPage.BUNDLES.ordinal -> {
                            showAddBundleDialog = true
                        }
                    }
                }
            ) { Icon(Icons.Default.Add, stringResource(R.string.add)) }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                DashboardPage.entries.forEachIndexed { index, page ->
                    HapticTab(
                        selected = pagerState.currentPage == index,
                        onClick = { composableScope.launch { pagerState.animateScrollToPage(index) } },
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
                        NotificationCard(
                            isWarning = true,
                            icon = Icons.Default.BatteryAlert,
                            text = stringResource(R.string.battery_optimization_notification),
                            onClick = {
                                androidContext.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${androidContext.packageName}")
                                })
                            }
                        )
                    }
                } else null,
                if (showNewDownloaderPluginsNotification) {
                    {
                        NotificationCard(
                            text = stringResource(R.string.new_downloader_plugins_notification),
                            icon = Icons.Outlined.Download,
                            modifier = Modifier.clickable(onClick = onDownloaderPluginClick),
                            actions = {
                                TextButton(onClick = vm::ignoreNewDownloaderPlugins) {
                                    Text(stringResource(R.string.dismiss))
                                }
                            }
                        )
                    }
                } else null
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize(),
                pageContent = { index ->
                    when (DashboardPage.entries[index]) {
                        DashboardPage.DASHBOARD -> {
                            InstalledAppsScreen(
                                onAppClick = onAppClick
                            )
                        }

                        DashboardPage.BUNDLES -> {
                            BackHandler {
                                if (bundlesSelectable) vm.cancelSourceSelection() else composableScope.launch {
                                    pagerState.animateScrollToPage(
                                        DashboardPage.DASHBOARD.ordinal
                                    )
                                }
                            }

                            val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())

                            BundleListScreen(
                                onDelete = {
                                    vm.delete(it)
                                },
                                onUpdate = {
                                    vm.update(it)
                                },
                                sources = sources,
                                selectedSources = vm.selectedSources,
                                bundlesSelectable = bundlesSelectable
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