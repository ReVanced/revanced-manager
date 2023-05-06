package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppScaffold
import app.revanced.manager.compose.ui.component.AppTopBar
import kotlinx.coroutines.launch

enum class DashboardPage(
    val titleResId: Int,
) {
    DASHBOARD(R.string.tab_apps),
    SOURCES(R.string.tab_sources),
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAppSelectorClick: () -> Unit
) {
    val pages: Array<DashboardPage> = DashboardPage.values()

    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "ReVanced Manager",
                actions = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                    }
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null)
                    }
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
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
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                pages.forEachIndexed { index, page ->
                    val title = stringResource(id = page.titleResId)
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = title) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                pageCount = pages.size,
                state = pagerState,
                userScrollEnabled = true,
                contentPadding = paddingValues,
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