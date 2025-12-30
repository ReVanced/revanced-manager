package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.viewmodel.SourceSelectorViewModel

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Filtering for ${viewModel.input.packageName}: ${viewModel.input.version}") }
            )
        }
    }
}
