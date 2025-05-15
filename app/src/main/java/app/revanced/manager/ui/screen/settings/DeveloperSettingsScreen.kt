package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DeveloperOptionsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onBackClick: () -> Unit,
    vm: DeveloperOptionsViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val prefs: PreferencesManager = koinInject()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.developer_options),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            GroupHeader(stringResource(R.string.manager))
            BooleanItem(
                preference = prefs.showDeveloperSettings,
                headline = R.string.developer_options,
                description = R.string.developer_options_description,
            )

            GroupHeader(stringResource(R.string.patch_bundles_section))
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_force_download),
                modifier = Modifier.clickable(onClick = vm::redownloadBundles)
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_reset),
                modifier = Modifier.clickable(onClick = vm::redownloadBundles)
            )
        }
    }
}