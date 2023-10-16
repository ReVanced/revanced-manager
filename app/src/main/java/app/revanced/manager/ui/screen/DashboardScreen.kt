package app.revanced.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.*
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
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.isDefault
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.bundle.BundleItem
import app.revanced.manager.ui.component.bundle.BundleTopBar
import app.revanced.manager.ui.component.bundle.ImportBundleDialog
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

enum class DashboardPage(
    val titleResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(R.string.tab_apps, Icons.Outlined.Apps),
    BUNDLES(R.string.tab_bundles, Icons.Outlined.Source),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = getViewModel(),
    onAppSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAppClick: (InstalledApp) -> Unit
) {
    var showImportBundleDialog by rememberSaveable { mutableStateOf(false) }

    val bundlesSelectable by remember { derivedStateOf { vm.selectedSources.size > 0 } }
    val pages: Array<DashboardPage> = DashboardPage.values()
    val availablePatches by vm.availablePatches.collectAsStateWithLifecycle(0)
    val androidContext = LocalContext.current

    val pagerState = rememberPagerState(
        initialPage = DashboardPage.DASHBOARD.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        DashboardPage.values().size
    }
    val composableScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != DashboardPage.BUNDLES.ordinal) vm.cancelSourceSelection()
    }

    if (showImportBundleDialog) {
        fun dismiss() {
            showImportBundleDialog = false
        }

        ImportBundleDialog(
            onDismissRequest = ::dismiss,
            onLocalSubmit = { name, patches, integrations ->
                dismiss()
                vm.createLocalSource(name, patches, integrations)
            },
            onRemoteSubmit = { name, url, autoUpdate ->
                dismiss()
                vm.createRemoteSource(name, url, autoUpdate)
            },
        )
    }

    Scaffold(
        topBar = {
            if (bundlesSelectable) {
                BundleTopBar(
                    title = stringResource(R.string.bundles_selected, vm.selectedSources.size),
                    onBackClick = vm::cancelSourceSelection,
                    onBackIcon = {
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
                            showImportBundleDialog = true
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add))
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                pages.forEachIndexed { index, page ->
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

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize(),
                pageContent = { index ->
                    when (pages[index]) {
                        DashboardPage.DASHBOARD -> {
                            InstalledAppsScreen(
                                onAppClick = onAppClick
                            )
                        }

                        DashboardPage.BUNDLES -> {

                            val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())

                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
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