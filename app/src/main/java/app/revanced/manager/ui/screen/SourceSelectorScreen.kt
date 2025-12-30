package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.viewmodel.SourceSelectorViewModel
import app.revanced.manager.util.enabled
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectorScreen(
    onBackClick: () -> Unit,
    onSave: (source: SelectedSource) -> Unit,
    viewModel: SourceSelectorViewModel,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Select source") },
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            HapticExtendedFloatingActionButton(
                text = { Text("Save") },
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = { onSave(viewModel.selectedSource) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            SourceOption(
                source = SelectedSource.Auto,
                isSelected = viewModel.selectedSource == SelectedSource.Auto,
                onSelect = viewModel::selectSource,
                headlineContent = { Text("Auto (Recommended)") },
                supportingContent = { Text("Automatically select the best available source") }
            )

            HorizontalDivider()

            SourceOption(
                source = SelectedSource.Installed,
                isSelected = viewModel.selectedSource == SelectedSource.Installed,
                onSelect = viewModel::selectSource,
                headlineContent = { Text("20.14.43") },
                supportingContent = { Text("Split APK's are not supported") },
                overlineContent = { Text("Installed") },
                enabled = false,
            )

            HorizontalDivider()

            SourceOption(
                source = SelectedSource.Downloaded("path"),
                isSelected = viewModel.selectedSource == SelectedSource.Downloaded("path"),
                onSelect = viewModel::selectSource,
                headlineContent = { Text("20.14.43") },
//                supportingContent = { Text("") },
                overlineContent = { Text("Downloaded") },

            )

            HorizontalDivider()

            SourceOption(
                source = SelectedSource.Plugin("plugin-id"),
                isSelected = viewModel.selectedSource == SelectedSource.Plugin("plugin-id"),
                onSelect = viewModel::selectSource,
                headlineContent = { Text("APKMirror Downloader") },
                overlineContent = { Text("Plugin") },
            )

        }
    }
}

@Composable
private fun SourceOption(
    source: SelectedSource,
    isSelected: Boolean,
    onSelect: (SelectedSource) -> Unit,
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled) { onSelect(source) }
            .enabled(enabled),
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        overlineContent = overlineContent,
        colors = transparentListItemColors
    )
}
