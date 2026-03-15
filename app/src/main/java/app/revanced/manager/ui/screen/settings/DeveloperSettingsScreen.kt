package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DeveloperOptionsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperSettingsScreen(
    onBackClick: () -> Unit,
    vm: DeveloperOptionsViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    val prefs: PreferencesManager = koinInject()

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.developer_options)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        ),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues),
            state = scrollState
        ) {
            ListSection(
                title = stringResource(R.string.manager),
                leadingContent = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                BooleanItem(
                    preference = prefs.showDeveloperSettings,
                    headline = R.string.developer_options,
                    description = R.string.developer_options_description,
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.reset_onboarding),
                    supportingContent = stringResource(R.string.reset_onboarding_description),
                    onClick = vm::resetOnboarding
                )
            }

            ListSection(
                title = stringResource(R.string.patches),
                leadingContent = { Icon(Icons.Outlined.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                SettingsListItem(
                    headlineContent = stringResource(R.string.patches_force_download),
                    onClick = vm::redownloadBundles
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.patches_reset),
                    onClick = vm::redownloadBundles
                )
            }
        }
    }
}