package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.NotificationCard
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.destination.SettingsDestination
import app.revanced.manager.ui.screen.settings.*
import app.revanced.manager.ui.screen.settings.update.ChangelogsScreen
import app.revanced.manager.ui.screen.settings.update.UpdateScreen
import app.revanced.manager.ui.screen.settings.update.UpdatesSettingsScreen
import app.revanced.manager.ui.viewmodel.SettingsViewModel
import dev.olshevski.navigation.reimagined.*
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import org.koin.androidx.compose.getViewModel as getComposeViewModel

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    startDestination: SettingsDestination,
    viewModel: SettingsViewModel = getViewModel()
) {
    val navController = rememberNavController(startDestination)

    val backClick: () -> Unit = {
        if (navController.backstack.entries.size == 1)
            onBackClick()
        else navController.pop()
    }

    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showBatteryButton by remember { mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName)) }

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
            R.string.advanced,
            R.string.advanced_description,
            Icons.Outlined.Tune
        ) to SettingsDestination.Advanced,
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
                onBackClick = backClick,
                viewModel = viewModel
            )

            is SettingsDestination.Advanced -> AdvancedSettingsScreen(
                onBackClick = backClick
            )

            is SettingsDestination.Updates -> UpdatesSettingsScreen(
                onBackClick = backClick,
                onChangelogClick = { navController.navigate(SettingsDestination.Changelogs) },
                onUpdateClick = { navController.navigate(SettingsDestination.Update(false)) }
            )

            is SettingsDestination.Downloads -> DownloadsSettingsScreen(
                onBackClick = backClick
            )

            is SettingsDestination.ImportExport -> ImportExportSettingsScreen(
                onBackClick = backClick
            )

            is SettingsDestination.About -> AboutSettingsScreen(
                onBackClick = backClick,
                onContributorsClick = { navController.navigate(SettingsDestination.Contributors) },
                onLicensesClick = { navController.navigate(SettingsDestination.Licenses) }
            )

            is SettingsDestination.Update -> UpdateScreen(
                onBackClick = backClick,
                vm = getComposeViewModel {
                    parametersOf(
                        destination.downloadOnScreenEntry
                    )
                }
            )

            is SettingsDestination.Changelogs -> ChangelogsScreen(
                onBackClick = backClick,
            )

            is SettingsDestination.Contributors -> ContributorScreen(
                onBackClick = backClick,
            )

            is SettingsDestination.Licenses -> LicensesScreen(
                onBackClick = backClick,
            )

            is SettingsDestination.Settings -> {
                Scaffold(
                    topBar = {
                        AppTopBar(
                            title = stringResource(R.string.settings),
                            onBackClick = backClick,
                        )
                    }
                ) { paddingValues ->
                    ColumnWithScrollbar(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    ) {
                        AnimatedVisibility(visible = showBatteryButton) {
                            NotificationCard(
                                isWarning = true,
                                icon = Icons.Default.BatteryAlert,
                                text = stringResource(R.string.battery_optimization_notification),
                                primaryAction = {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    })
                                    showBatteryButton =
                                        !pm.isIgnoringBatteryOptimizations(context.packageName)
                                }
                            )
                        }
                        settingsSections.forEach { (titleDescIcon, destination) ->
                            SettingsListItem(
                                modifier = Modifier.clickable { navController.navigate(destination) },
                                headlineContent = stringResource(titleDescIcon.first),
                                supportingContent = stringResource(titleDescIcon.second),
                                leadingContent = { Icon(titleDescIcon.third, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}