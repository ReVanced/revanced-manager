package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.viewmodel.InstalledAppsViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun InstalledAppsScreen(
    onAppClick: (InstalledApp) -> Unit,
    viewModel: InstalledAppsViewModel = getViewModel()
) {
    val installedApps by viewModel.apps.collectAsStateWithLifecycle(initialValue = null)

    Column {
        if (!Aapt.supportsDevice()) {
            NotificationCard(
                isWarning = true,
                icon = Icons.Outlined.WarningAmber,
                text = stringResource(
                    R.string.unsupported_architecture_warning
                ),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (installedApps.isNullOrEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            installedApps?.let { installedApps ->

                if (installedApps.isNotEmpty()) {
                    items(
                        installedApps,
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
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.no_patched_apps_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

            } ?: item { LoadingIndicator() }
        }
    }
}