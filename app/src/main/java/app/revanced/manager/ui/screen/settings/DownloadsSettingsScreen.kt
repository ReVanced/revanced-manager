package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = getViewModel()
) {
    val prefs = viewModel.prefs

    val downloadedApps by viewModel.downloadedApps.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.downloads),
                onBackClick = onBackClick,
                actions = {
                    if (viewModel.selection.isNotEmpty()) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BooleanItem(
                preference = prefs.preferSplits,
                headline = R.string.prefer_splits,
                description = R.string.prefer_splits_description,
            )

            GroupHeader(stringResource(R.string.downloaded_apps))

            downloadedApps.forEach { app ->
                val selected = app in viewModel.selection

                SettingsListItem(
                    modifier = Modifier.clickable { viewModel.toggleItem(app) },
                    headlineContent = app.packageName,
                    leadingContent = (@Composable {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { viewModel.toggleItem(app) }
                        )
                    }).takeIf { viewModel.selection.isNotEmpty() },
                    supportingContent = app.version,
                    tonalElevation = if (selected) 8.dp else 0.dp
                )
            }
        }
    }
}