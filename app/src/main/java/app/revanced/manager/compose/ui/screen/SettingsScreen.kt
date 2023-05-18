package app.revanced.manager.compose.ui.screen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.destination.SettingsDestination
import app.revanced.manager.compose.ui.screen.settings.AboutSettingsScreen
import app.revanced.manager.compose.ui.screen.settings.DownloadsSettingsScreen
import app.revanced.manager.compose.ui.screen.settings.GeneralSettingsScreen
import app.revanced.manager.compose.ui.screen.settings.ImportExportSettingsScreen
import app.revanced.manager.compose.ui.screen.settings.UpdatesSettingsScreen
import app.revanced.manager.compose.ui.viewmodel.SettingsViewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = getViewModel()
) {
    val navController = rememberNavController<SettingsDestination>(startDestination = SettingsDestination.Settings)

    NavBackHandler(navController)

    AnimatedNavHost(
        controller = navController
    ) { destination ->
        when (destination) {

            is SettingsDestination.General -> GeneralSettingsScreen(
                onBackClick = { navController.pop() },
                viewModel = viewModel
            )

            is SettingsDestination.Updates -> UpdatesSettingsScreen(
                onBackClick = { navController.pop() }
            )

            is SettingsDestination.Downloads -> DownloadsSettingsScreen(
                onBackClick = { navController.pop() }
            )

            is SettingsDestination.ImportExport -> ImportExportSettingsScreen(
                onBackClick = { navController.pop() }
            )

            is SettingsDestination.About -> AboutSettingsScreen(
                onBackClick = { navController.pop() }
            )

            is SettingsDestination.Settings -> {
                Scaffold(
                    topBar = {
                        AppTopBar(
                            title = stringResource(R.string.settings),
                            onBackClick = onBackClick,
                            actions = {

                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(SettingsDestination.General) },
                            leadingContent = { Icon(Icons.Outlined.Settings, null) },
                            headlineContent = { Text(stringResource(R.string.general)) },
                            supportingContent = { Text(stringResource(R.string.general_description)) }
                        )

                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(SettingsDestination.Updates) },
                            leadingContent = { Icon(Icons.Outlined.Update, null) },
                            headlineContent = { Text(stringResource(R.string.updates)) },
                            supportingContent = { Text(stringResource(R.string.updates_description)) }
                        )

                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(SettingsDestination.Downloads) },
                            leadingContent = { Icon(Icons.Outlined.Download, null) },
                            headlineContent = { Text(stringResource(R.string.downloads)) },
                            supportingContent = { Text(stringResource(R.string.downloads_description)) }
                        )

                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(SettingsDestination.ImportExport) },
                            leadingContent = { Icon(Icons.Outlined.ImportExport, null) },
                            headlineContent = { Text(stringResource(R.string.import_export)) },
                            supportingContent = { Text(stringResource(R.string.import_export_description)) }
                        )

                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(SettingsDestination.About) },
                            leadingContent = { Icon(Icons.Outlined.Info, null) },
                            headlineContent = { Text(stringResource(R.string.about)) },
                            supportingContent = { Text(stringResource(R.string.about_description)) }
                        )

                    }
                }
            }
        }
    }
}