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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.destination.SettingsDestination
import app.revanced.manager.compose.ui.screen.settings.*
import app.revanced.manager.compose.ui.viewmodel.SettingsViewModel
import dev.olshevski.navigation.reimagined.*
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
@Stable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = getViewModel()
) {
    val navController =
        rememberNavController<SettingsDestination>(startDestination = SettingsDestination.Settings)
    val settingsSections = listOf(
        Triple(
            R.string.general,
            R.string.general_description,
            Icons.Outlined.Settings
        ) to SettingsDestination.General,
        Triple(
            R.string.updates,
            R.string.updates_description,
            Icons.Outlined.Update
        ) to SettingsDestination.Updates,
        Triple(
            R.string.downloads,
            R.string.downloads_description,
            Icons.Outlined.Download
        ) to SettingsDestination.Downloads,
        Triple(
            R.string.import_export,
            R.string.import_export_description,
            Icons.Outlined.SwapVert
        ) to SettingsDestination.ImportExport,
        Triple(
            R.string.about,
            R.string.about_description,
            Icons.Outlined.Info
        ) to SettingsDestination.About,
    )
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
                        settingsSections.forEach { (titleDescIcon, destination) ->
                            ListItem(
                                modifier = Modifier.clickable { navController.navigate(destination) },
                                headlineContent = { Text(stringResource(titleDescIcon.first)) },
                                supportingContent = { Text(stringResource(titleDescIcon.second)) },
                                leadingContent = { Icon(titleDescIcon.third, null) }
                            )
                        }
                    }
                }
            }
        }
}
}