package app.revanced.manager.ui.screen

import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.ExpressiveListIcon
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.model.navigation.Settings
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.koin.compose.koinInject

private data class Section(
    @param:StringRes val name: Int,
    @param:StringRes val description: Int,
    val image: ImageVector,
    val destination: Settings.Destination,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, navigate: (Settings.Destination) -> Unit) {
    val prefs: PreferencesManager = koinInject()
    val showDeveloperSettings by prefs.showDeveloperSettings.getAsState()

    val context = LocalContext.current
    val appIcon = rememberDrawablePainter(
        drawable = remember(context) {
            AppCompatResources.getDrawable(context, R.drawable.ic_logo_ring)
        }
    )

    val generalSections = remember {
        listOf(
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
            )
        )
    }

    val advancedSections = remember {
        listOf(
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
            )
        )
    }

    val developerSection = remember(showDeveloperSettings) {
        Section(
            R.string.developer_options,
            R.string.developer_options_description,
            Icons.Outlined.Code,
            Settings.Developer
        ).takeIf { showDeveloperSettings }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                SettingsListItem(
                    modifier = Modifier.clip(MaterialTheme.shapes.large),
                    headlineContent = stringResource(
                        R.string.about_app_name,
                        stringResource(R.string.app_name)
                    ),
                    supportingContent = BuildConfig.VERSION_NAME,
                    leadingContent = {
                        Image(
                            painter = appIcon,
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(42.dp)
                        )
                    },
                    onClick = { navigate(Settings.About) }
                )
            }
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ListSection {
                    generalSections.forEach { (name, description, icon, destination) ->
                        SettingsListItem(
                            headlineContent = stringResource(name),
                            supportingContent = stringResource(description),
                            leadingContent = { ExpressiveListIcon(icon = icon) },
                            onClick = { navigate(destination) }
                        )
                    }
                }

                ListSection {
                    advancedSections.forEach { (name, description, icon, destination) ->
                        SettingsListItem(
                            headlineContent = stringResource(name),
                            supportingContent = stringResource(description),
                            leadingContent = { ExpressiveListIcon(icon = icon) },
                            onClick = { navigate(destination) }
                        )
                    }
                }

                developerSection?.let { (name, description, icon, destination) ->
                    ListSection {
                        SettingsListItem(
                            headlineContent = stringResource(name),
                            supportingContent = stringResource(description),
                            leadingContent = { ExpressiveListIcon(icon = icon) },
                            onClick = { navigate(destination) }
                        )
                    }
                }
            }
        }
    }
}