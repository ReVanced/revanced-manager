package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.InstalledAppsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun InstalledAppsScreen(
    onAppClick: (InstalledApp) -> Unit,
    viewModel: InstalledAppsViewModel = koinViewModel()
) {
    val installedApps by viewModel.apps.collectAsStateWithLifecycle(initialValue = null)

    when {
        installedApps == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }

        installedApps.orEmpty().isEmpty() -> Box(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Outlined.Apps,
                title = R.string.no_patched_apps_found,
                description = R.string.no_patched_apps_description
            )
        }

        else -> {
            Column {
                LazyColumnWithScrollbar(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    items(
                        installedApps.orEmpty(),
                        key = { it.currentPackageName }
                    ) { installedApp ->
                        viewModel.packageInfoMap[installedApp.currentPackageName].let { packageInfo ->
                            ListItem(
                                modifier = Modifier.clickable { onAppClick(installedApp) },
                                leadingContent = {
                                    AppIcon(
                                        packageInfo,
                                        contentDescription = null,
                                        Modifier.size(36.dp)
                                    )
                                },
                                headlineContent = { AppLabel(packageInfo, defaultText = null) },
                                supportingContent = { Text(installedApp.currentPackageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}