package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
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
    val localVersion by viewModel.localVersion.collectAsStateWithLifecycle(null)

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.version_selector_title)) },
                onBackClick = onBackClick
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
        LazyColumnWithScrollbar(contentPadding = paddingValues) {
            item {
                VersionOption(
                    version = SelectedVersion.Auto,
                    isSelected = viewModel.selectedVersion is SelectedVersion.Auto,
                    onSelect = viewModel::selectVersion,
                    headlineContent = { Text(stringResource(R.string.version_selector_auto_title)) },
                    supportingContent = { Text(stringResource(R.string.version_selector_auto_description)) }
                )
                HorizontalDivider()
            }

            if (versions.isNotEmpty()) {
                items(versions, key = { (version, _) -> version.version }) { (version, incompatibleCount) ->
                    val overlineText = when {
                        localVersion == version.version -> stringResource(R.string.version_selector_state_local)
                        version.version in downloadedVersions && viewModel.installedAppVersion == version.version ->
                            stringResource(R.string.version_selector_state_downloaded_installed)
                        version.version in downloadedVersions -> stringResource(R.string.version_selector_state_downloaded)
                        viewModel.installedAppVersion == version.version -> stringResource(R.string.version_selector_state_installed)
                        else -> null
                    }

                    VersionOption(
                        version = version,
                        isSelected = viewModel.selectedVersion == version,
                        onSelect = viewModel::selectVersion,
                        headlineContent = { Text(version.version) },
                        supportingContent = {
                            Text(
                                if (incompatibleCount == 0) stringResource(R.string.version_selector_all_patches_compatible)
                                else pluralStringResource(R.plurals.version_selector_incompatible_patches, incompatibleCount, incompatibleCount)
                            )
                        },
                        overlineContent = overlineText?.let { { Text(it) } }
                    )
                }
            } else {
                item {
                    VersionOption(
                        version = SelectedVersion.Any,
                        isSelected = viewModel.selectedVersion is SelectedVersion.Any,
                        onSelect = viewModel::selectVersion,
                        headlineContent = { Text(stringResource(R.string.version_selector_any_title)) },
                        supportingContent = { Text(stringResource(R.string.version_selector_any_description)) }
                    )
                }
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
        modifier = Modifier.clickable { onSelect(version) },
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
