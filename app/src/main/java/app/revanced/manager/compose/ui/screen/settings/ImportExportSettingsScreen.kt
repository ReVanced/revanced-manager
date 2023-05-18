package app.revanced.manager.compose.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.component.GroupHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSettingsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.import_export),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            GroupHeader(stringResource(R.string.signing))
            ListItem(
                modifier = Modifier.clickable {  },
                headlineContent = { Text(stringResource(R.string.import_keystore)) },
                supportingContent = { Text(stringResource(R.string.import_keystore_descripion)) }
            )
            ListItem(
                modifier = Modifier.clickable {  },
                headlineContent = { Text(stringResource(R.string.export_keystore)) },
                supportingContent = { Text(stringResource(R.string.export_keystore_description)) }
            )
        }
    }
}