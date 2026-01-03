package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.ui.viewmodel.VersionSelectorViewModel
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectorScreen(
    onBackClick: () -> Unit,
    onSave: (version: SelectedVersion) -> Unit,
    viewModel: VersionSelectorViewModel,
) {
    val versions by viewModel.availableVersions.collectAsStateWithLifecycle(emptyList())
    val downloadedVersions by viewModel.downloadedVersions.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Select version") },
                onBackClick = onBackClick,
                actions = {
                    IconButton({}) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.save)) },
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                onClick = { onSave(viewModel.selectedVersion) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            VersionOption(
                version = SelectedVersion.Auto,
                isSelected = viewModel.selectedVersion is SelectedVersion.Auto,
                onSelect = viewModel::selectVersion,
                headlineContent = { Text("Auto (Recommended)") },
                supportingContent = { Text("Automatically select the best available version") }
            )
            HorizontalDivider()

            if (versions.isNotEmpty()) {
                LazyColumn {
                    items(versions, key = { it.first.version }) { version ->
                        val isDownloaded = downloadedVersions.contains(version.first.version)
                        val isInstalled = viewModel.installedAppVersion == version.first.version

                        val overlineText = when {
                            isDownloaded && isInstalled -> "Downloaded, Installed"
                            isDownloaded -> "Downloaded"
                            isInstalled -> "Installed"
                            else -> null
                        }

                        VersionOption(
                            version = version.first,
                            isSelected = viewModel.selectedVersion == version.first,
                            onSelect = viewModel::selectVersion,
                            headlineContent = { Text(version.first.version) },
                            supportingContent = {
                                Text(
                                    "${version.second.let { if (it == 0) "No" else it }} incompatible patches"
                                )
                            },
                            overlineContent = overlineText?.let { { Text(it) } }
                        )
                    }
                }
            } else {
                VersionOption(
                    version = SelectedVersion.Any,
                    isSelected = viewModel.selectedVersion is SelectedVersion.Any,
                    onSelect = viewModel::selectVersion,
                    headlineContent = { Text("Any available version") },
                    supportingContent = { Text("Use any available version regardless of compatibility") }
                )
            }
        }
    }
}

@Composable
private fun VersionOption(
    version: SelectedVersion,
    isSelected: Boolean,
    onSelect: (SelectedVersion) -> Unit,
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable { onSelect(version) },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = overlineContent,
        colors = transparentListItemColors
    )
}
