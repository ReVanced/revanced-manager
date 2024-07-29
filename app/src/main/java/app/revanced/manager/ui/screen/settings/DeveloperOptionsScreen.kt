package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DeveloperOptionsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(
    onBackClick: () -> Unit,
    vm: DeveloperOptionsViewModel = koinViewModel()
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.developer_options),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            GroupHeader(stringResource(R.string.patch_bundles_section))
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_redownload),
                modifier = Modifier.clickable(onClick = vm::redownloadBundles)
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_reset),
                modifier = Modifier.clickable(onClick = vm::redownloadBundles)
            )
        }
    }
}