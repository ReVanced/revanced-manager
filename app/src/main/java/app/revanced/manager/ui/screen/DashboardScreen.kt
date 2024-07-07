package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.AutoUpdatesDialog
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.bundle.BundleItem
import app.revanced.manager.ui.component.bundle.BundleTopBar
import app.revanced.manager.ui.component.bundle.ImportBundleDialog
import app.revanced.manager.ui.component.bundle.ImportBundleTypeSelectorDialog
import app.revanced.manager.ui.model.BundleType
import app.revanced.manager.ui.viewmodel.DashboardViewModel
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    onAppSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAppClick: (InstalledApp) -> Unit
) {
    val bundlesSelectable by remember { derivedStateOf { vm.selectedSources.size > 0 } }
    val availablePatches by vm.availablePatches.collectAsStateWithLifecycle(0)
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

    var selectedBundleType: BundleType? by rememberSaveable { mutableStateOf(null) }
    selectedBundleType?.let {
        fun dismiss() {
            selectedBundleType = null
        }

        ImportBundleDialog(
            onDismissRequest = ::dismiss,
            onLocalSubmit = { patches, integrations ->
                dismiss()
                vm.createLocalSource(patches, integrations)
            },
            onRemoteSubmit = { url, autoUpdate ->
                dismiss()
                vm.createRemoteSource(url, autoUpdate)
            },
            initialBundleType = it
        )
    }

    var showBundleTypeSelectorDialog by rememberSaveable { mutableStateOf(false) }
    if (showBundleTypeSelectorDialog) {
        ImportBundleTypeSelectorDialog(
            onDismiss = { showBundleTypeSelectorDialog = false },
            onConfirm = {
                selectedBundleType = it
                showBundleTypeSelectorDialog = false
            }
        )
    }

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
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
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
                                return@FloatingActionButton
                            }

                            onAppSelectorClick()
                        }

                        DashboardPage.BUNDLES.ordinal -> {
                            showBundleTypeSelectorDialog = true
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
                    Tab(
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
                vm.updatedManagerVersion?.let {
                    {
                        NotificationCard(
                            text = stringResource(R.string.update_available_dialog_description, it),
                            icon = Icons.Outlined.Update,
                            actions = {
                                TextButton(onClick = vm::dismissUpdateDialog) {
                                    Text(stringResource(R.string.dismiss))
                                }
                                TextButton(onClick = onUpdateClick) {
                                    Text(stringResource(R.string.update))
                                }
                            }
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

                            Column(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                sources.forEach {
                                    BundleItem(
                                        bundle = it,
                                        onDelete = {
                                            vm.delete(it)
                                        },
                                        onUpdate = {
                                            vm.update(it)
                                        },
                                        selectable = bundlesSelectable,
                                        onSelect = {
                                            vm.selectedSources.add(it)
                                        },
                                        isBundleSelected = vm.selectedSources.contains(it),
                                        toggleSelection = { bundleIsNotSelected ->
                                            if (bundleIsNotSelected) {
                                                vm.selectedSources.add(it)
                                            } else {
                                                vm.selectedSources.remove(it)
                                            }
                                        }
                                    )
                                }
                            }
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