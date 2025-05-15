package app.revanced.manager.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.model.navigation.Settings
import org.koin.compose.koinInject

private data class Section(
    @StringRes val name: Int,
    @StringRes val description: Int,
    val image: ImageVector,
    val destination: Settings.Destination,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, navigate: (Settings.Destination) -> Unit) {
    val prefs: PreferencesManager = koinInject()
    val showDeveloperSettings by prefs.showDeveloperSettings.getAsState()

    val settingsSections = remember(showDeveloperSettings) {
        listOfNotNull(
            Section(
                R.string.general,
                R.string.general_description,
                Icons.Outlined.Settings,
                Settings.General
            ),
            Section(
                R.string.updates,
                R.string.updates_description,
                Icons.Outlined.Update,
                Settings.Updates
            ),
            Section(
                R.string.downloads,
                R.string.downloads_description,
                Icons.Outlined.Download,
                Settings.Downloads
            ),
            Section(
                R.string.import_export,
                R.string.import_export_description,
                Icons.Outlined.SwapVert,
                Settings.ImportExport
            ),
            Section(
                R.string.advanced,
                R.string.advanced_description,
                Icons.Outlined.Tune,
                Settings.Advanced
            ),
            Section(
                R.string.about,
                R.string.app_name,
                Icons.Outlined.Info,
                Settings.About
            ),
            Section(
                R.string.developer_options,
                R.string.developer_options_description,
                Icons.Outlined.Code,
                Settings.Developer
            ).takeIf { showDeveloperSettings }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings),
                onBackClick = onBackClick,
            )
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            settingsSections.forEach { (name, description, icon, destination) ->
                SettingsListItem(
                    modifier = Modifier.clickable { navigate(destination) },
                    headlineContent = stringResource(name),
                    supportingContent = stringResource(description),
                    leadingContent = { Icon(icon, null) }
                )
            }
        }
    }
}