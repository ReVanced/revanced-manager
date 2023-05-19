package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import kotlinx.coroutines.launch

enum class DashboardPage(
    val titleResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(R.string.tab_apps, Icons.Outlined.Apps),
    SOURCES(R.string.tab_sources, Icons.Outlined.Source),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAppSelectorClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val pages: Array<DashboardPage> = DashboardPage.values()

    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (pagerState.currentPage == DashboardPage.DASHBOARD.ordinal)
                    onAppSelectorClick()
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
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(page.titleResId)) },
                        icon = { Icon(page.icon, null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                pageCount = pages.size,
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->
                    when (pages[index]) {
                        DashboardPage.DASHBOARD -> {
                            InstalledAppsScreen()
                        }

                        DashboardPage.SOURCES -> {
                            SourcesScreen()
                        }
                    }
                }
            )
        }
    }
}