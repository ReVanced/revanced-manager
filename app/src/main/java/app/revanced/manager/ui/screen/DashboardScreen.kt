package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AvailableUpdateDialog
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.PillTab
import app.revanced.manager.ui.component.PillTabBar
import app.revanced.manager.ui.component.bundle.BundleTopBar
import app.revanced.manager.ui.component.bundle.ImportPatchBundleDialog
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.ui.viewmodel.DownloaderUpdateState
import app.revanced.manager.util.RequestInstallAppsContract
import app.revanced.manager.util.toast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

enum class DashboardPage(
    val titleResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(R.string.tab_apps, Icons.Outlined.Apps),
    BUNDLES(R.string.tab_patches, Icons.Outlined.Source),
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    onAppSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
    onAnnouncementClick: (ReVancedAnnouncement) -> Unit,
    onDownloaderClick: () -> Unit,
    onAppClick: (String) -> Unit,
    onPatchableAppClick: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
    onBundleClick: (Int) -> Unit
) {
    var selectedSourceCount by rememberSaveable { mutableIntStateOf(0) }
    val bundlesSelectable by remember { derivedStateOf { selectedSourceCount > 0 } }
    val availablePatches by vm.availablePatches.collectAsStateWithLifecycle(0)
    val bundleDownloadError by vm.bundleDownloadError.collectAsStateWithLifecycle(null)
    val showNewDownloaderNotification by vm.newDownloadersAvailable.collectAsStateWithLifecycle(false)
    val managerAutoUpdates by vm.prefs.managerAutoUpdates.getAsState()
    val showManagerUpdateDialogOnLaunch by vm.prefs.showManagerUpdateDialogOnLaunch.getAsState()
    val availableUpdate by vm.availableManagerUpdate.collectAsStateWithLifecycle()
    val androidContext = LocalContext.current
    val resources = LocalResources.current
    val logoPainter = rememberDrawablePainter(drawable = remember(resources) {
        AppCompatResources.getDrawable(androidContext, R.drawable.ic_logo_ring)
    })
    val composableScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = DashboardPage.DASHBOARD.ordinal,
        initialPageOffsetFraction = 0f
    ) { DashboardPage.entries.size }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != DashboardPage.BUNDLES.ordinal) vm.cancelSourceSelection()
    }

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
    if (managerAutoUpdates && showUpdateDialog && showManagerUpdateDialogOnLaunch && availableUpdate != null) {
        AvailableUpdateDialog(
            onDismiss = { showUpdateDialog = false },
            setShowManagerUpdateDialogOnLaunch = vm::setShowManagerUpdateDialogOnLaunch,
            onConfirm = onUpdateClick,
            newVersion = availableUpdate!!
        )
    }

    val downloaderUpdate = vm.availableDownloaderUpdate
    val downloaderUpdateState = vm.downloaderUpdateState
    if (downloaderUpdate != null || downloaderUpdateState == DownloaderUpdateState.DOWNLOADING || downloaderUpdateState == DownloaderUpdateState.INSTALLING) {
        AlertDialogExtended(
            onDismissRequest = {
                if (downloaderUpdateState != DownloaderUpdateState.DOWNLOADING && downloaderUpdateState != DownloaderUpdateState.INSTALLING) {
                    vm.dismissDownloaderUpdate()
                }
            },
            confirmButton = {
                when (downloaderUpdateState) {
                    DownloaderUpdateState.IDLE -> {
                        TextButton(onClick = vm::downloadAndInstallDownloaderUpdate, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.update))
                        }
                    }
                    DownloaderUpdateState.FAILED -> {
                        TextButton(onClick = vm::downloadAndInstallDownloaderUpdate, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (downloaderUpdateState != DownloaderUpdateState.DOWNLOADING && downloaderUpdateState != DownloaderUpdateState.INSTALLING) {
                    TextButton(onClick = vm::dismissDownloaderUpdate, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            },
            icon = {
                Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
            },
            title = {
                Text(stringResource(R.string.downloader_update_available))
            },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.downloader_update_available_description,
                            downloaderUpdate?.version.orEmpty()
                        )
                    )
                    if (downloaderUpdateState == DownloaderUpdateState.DOWNLOADING) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearWavyProgressIndicator(
                            progress = { vm.downloaderUpdateProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (downloaderUpdateState == DownloaderUpdateState.INSTALLING) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.api_downloader_installing))
                    }
                    if (downloaderUpdateState == DownloaderUpdateState.FAILED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.api_downloader_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    var pendingAppSelectorLaunch by rememberSaveable { mutableStateOf(false) }
    var pendingPatchablePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingStorageSelection by rememberSaveable { mutableStateOf<SelectedApp.Local?>(null) }

    fun clearPendingSelection() {
        pendingAppSelectorLaunch = false
        pendingPatchablePackage = null
        pendingStorageSelection = null
    }

    fun resumePendingSelection() {
        if (pendingAppSelectorLaunch) {
            clearPendingSelection()
            onAppSelectorClick()
            return
        }

        pendingPatchablePackage?.let {
            clearPendingSelection()
            onPatchableAppClick(it)
            return
        }

        pendingStorageSelection?.let {
            clearPendingSelection()
            onStorageSelect(it)
        }
    }

    var showAndroid11Dialog by rememberSaveable { mutableStateOf(false) }
    val installAppsPermissionLauncher =
        rememberLauncherForActivityResult(RequestInstallAppsContract) { granted ->
            showAndroid11Dialog = false
            if (granted) {
                resumePendingSelection()
            } else {
                clearPendingSelection()
            }
        }

    if (showAndroid11Dialog) {
        Android11Dialog(
            onDismissRequest = {
                showAndroid11Dialog = false
                clearPendingSelection()
            },
            onContinue = {
                installAppsPermissionLauncher.launch(androidContext.packageName)
            }
        )
    }

    fun onPatchableSelection(packageName: String) {
        if (vm.android11BugActive) {
            clearPendingSelection()
            pendingPatchablePackage = packageName
            showAndroid11Dialog = true
            return
        }

        onPatchableAppClick(packageName)
    }

    fun onStorageSelection(app: SelectedApp.Local) {
        if (vm.android11BugActive) {
            clearPendingSelection()
            pendingStorageSelection = app
            showAndroid11Dialog = true
            return
        }

        onStorageSelect(app)
    }

    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = vm::deleteSources,
            title = stringResource(R.string.delete),
            description = stringResource(R.string.patches_delete_multiple_dialog_description),
            icon = Icons.Outlined.Delete
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 96.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            if (navBarHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(navBarHeight)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
            }

            Scaffold(
                topBar = {
                    if (bundlesSelectable) {
                        BundleTopBar(
                            title = stringResource(R.string.patches_selected, selectedSourceCount),
                            onBackClick = vm::cancelSourceSelection,
                            backIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.back)
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showDeleteConfirmationDialog = true },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                                }
                                IconButton(
                                    onClick = vm::updateSources,
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(Icons.Filled.Refresh, stringResource(R.string.refresh))
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        painter = logoPainter,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(stringResource(R.string.app_name))
                                }
                            },
                            actions = {
                                if (availableUpdate != null) {
                                    IconButton(
                                        onClick = onUpdateClick,
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        BadgedBox(badge = { Badge(modifier = Modifier.size(6.dp)) }) {
                                            Icon(Icons.Filled.Update, stringResource(R.string.update))
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = onAnnouncementsClick,
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (vm.unreadAnnouncement != null) {
                                                Badge(modifier = Modifier.size(6.dp))
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Notifications,
                                            stringResource(R.string.announcements)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onSettingsClick,
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                containerColor = Color.Transparent,
                floatingActionButton = {
                    DashboardFab(
                        pagerState = pagerState,
                        onPatchAppClick = {
                            vm.cancelSourceSelection()
                            if (availablePatches < 1) {
                                androidContext.toast(resources.getString(R.string.no_patch_found))
                                composableScope.launch {
                                    pagerState.animateScrollToPage(DashboardPage.BUNDLES.ordinal)
                                }
                                return@DashboardFab
                            }
                            if (vm.android11BugActive) {
                                clearPendingSelection()
                                pendingAppSelectorLaunch = true
                                showAndroid11Dialog = true
                                return@DashboardFab
                            }
                            onAppSelectorClick()
                        },
                        onAddBundleClick = {
                            vm.cancelSourceSelection()
                            showAddBundleDialog = true
                        }
                    )
                }
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues)) {
                    if (!bundlesSelectable) {
                        PillTabBar(
                            pagerState = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            DashboardPage.entries.forEachIndexed { index, page ->
                                PillTab(
                                    index = index,
                                    onClick = { composableScope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(stringResource(page.titleResId)) },
                                    icon = { Icon(page.icon, null) }
                                )
                            }
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
                        if (bundleDownloadError != null) {
                            {
                                NotificationCard(
                                    isWarning = true,
                                    icon = Icons.Outlined.WarningAmber,
                                    title = stringResource(R.string.api_not_working_title),
                                    text = stringResource(R.string.api_not_working_description),
                                    onClick = onSettingsClick
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
                                        TextButton(
                                            onClick = vm::ignoreNewDownloaders,
                                            shapes = ButtonDefaults.shapes()
                                        ) {
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
                                        TextButton(
                                            onClick = vm::markUnreadAnnouncementRead,
                                            shapes = ButtonDefaults.shapes()
                                        ) {
                                            Text(stringResource(R.string.dismiss))
                                        }
                                        TextButton(
                                            onClick = {
                                                vm.markUnreadAnnouncementRead()
                                                onAnnouncementClick(announcement)
                                            },
                                            shapes = ButtonDefaults.shapes()
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
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        when (DashboardPage.entries[index]) {
                            DashboardPage.DASHBOARD -> {
                                InstalledAppsScreen(
                                    onAppClick = { onAppClick(it.currentPackageName) },
                                    onPatchableAppClick = ::onPatchableSelection,
                                    onStorageSelect = { selectedApp -> onStorageSelection(selectedApp) }
                                )
                            }

                            DashboardPage.BUNDLES -> {
                                BackHandler {
                                    if (bundlesSelectable) {
                                        vm.cancelSourceSelection()
                                    } else {
                                        composableScope.launch {
                                            pagerState.animateScrollToPage(DashboardPage.DASHBOARD.ordinal)
                                        }
                                    }
                                }

                                BundleListScreen(
                                    eventsFlow = vm.bundleListEventsFlow,
                                    setSelectedSourceCount = { selectedSourceCount = it },
                                    onBundleClick = onBundleClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardFab(
    pagerState: PagerState,
    onPatchAppClick: () -> Unit,
    onAddBundleClick: () -> Unit
) {
    val swipeProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)

    HapticExtendedFloatingActionButton(
        onClick = {
            when (pagerState.currentPage) {
                DashboardPage.DASHBOARD.ordinal -> onPatchAppClick()
                DashboardPage.BUNDLES.ordinal -> onAddBundleClick()
            }
        },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { FabTextCrossfade(swipeProgress) }
    )
}

@Composable
private fun FabTextCrossfade(progress: Float) {
    val texts = listOf(
        stringResource(R.string.fab_patch_app),
        stringResource(R.string.fab_add_patches)
    )

    Layout(
        content = {
            texts.forEachIndexed { index, text ->
                val textProgress = if (index == 0) 1f - progress else progress
                val direction = if (index == 0) 1f else -1f
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.graphicsLayer {
                        alpha = textProgress
                        translationX = (1f - textProgress) * direction * -50f
                    }
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = lerp(placeables[0].width.toFloat(), placeables[1].width.toFloat(), progress).toInt()
        val height = placeables.maxOf { it.height }

        layout(width, height) {
            placeables.forEach { it.placeRelative(0, 0) }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Android11Dialog(onDismissRequest: () -> Unit, onContinue: () -> Unit) {
    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onContinue, shapes = ButtonDefaults.shapes()) {
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
