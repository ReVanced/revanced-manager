package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.ui.component.AutoUpdatesDialog
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.destination.SettingsDestination
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstalledAppInfoScreen
import app.revanced.manager.ui.screen.InstallerScreen
import app.revanced.manager.ui.screen.SelectedAppInfoScreen
import app.revanced.manager.ui.screen.SettingsScreen
import app.revanced.manager.ui.screen.VersionSelectorScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import org.koin.core.parameter.parametersOf
import org.koin.androidx.compose.getViewModel as getComposeViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel as getAndroidViewModel

class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val vm: MainViewModel = getAndroidViewModel()

        vm.importLegacySettings(this)

        setContent {
            val theme by vm.prefs.theme.getAsState()
            val dynamicColor by vm.prefs.dynamicColor.getAsState()

            ReVancedManagerTheme(
                darkTheme = theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK,
                dynamicColor = dynamicColor
            ) {
                val navController =
                    rememberNavController<Destination>(startDestination = Destination.Dashboard)

                NavBackHandler(navController)

                val firstLaunch by vm.prefs.firstLaunch.getAsState()

                if (firstLaunch) AutoUpdatesDialog(vm::applyAutoUpdatePrefs)

                vm.updatedManagerVersion?.let {
                    AlertDialog(
                        onDismissRequest = vm::dismissUpdateDialog,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    vm.dismissUpdateDialog()
                                    navController.navigate(Destination.Settings(SettingsDestination.Update(false)))
                                }
                            ) {
                                Text(stringResource(R.string.update))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = vm::dismissUpdateDialog) {
                                Text(stringResource(R.string.dismiss_temporary))
                            }
                        },
                        icon = { Icon(Icons.Outlined.Update, null) },
                        title = { Text(stringResource(R.string.update_available_dialog_title)) },
                        text = { Text(stringResource(R.string.update_available_dialog_description, it)) }
                    )
                }

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onSettingsClick = { navController.navigate(Destination.Settings()) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) },
                            onAppClick = { installedApp ->
                                navController.navigate(
                                    Destination.InstalledApplicationInfo(
                                        installedApp
                                    )
                                )
                            }
                        )

                        is Destination.InstalledApplicationInfo -> InstalledAppInfoScreen(
                            onPatchClick = { packageName, patchesSelection ->
                                navController.navigate(
                                    Destination.VersionSelector(
                                        packageName,
                                        patchesSelection
                                    )
                                )
                            },
                            onBackClick = { navController.pop() },
                            viewModel = getComposeViewModel { parametersOf(destination.installedApp) }
                        )

                        is Destination.Settings -> SettingsScreen(
                            onBackClick = { navController.pop() },
                            startDestination = destination.startDestination
                        )

                        is Destination.AppSelector -> AppSelectorScreen(
                            onAppClick = { navController.navigate(Destination.VersionSelector(it)) },
                            onStorageClick = {
                                navController.navigate(
                                    Destination.SelectedApplicationInfo(
                                        it
                                    )
                                )
                            },
                            onBackClick = { navController.pop() }
                        )

                        is Destination.VersionSelector -> VersionSelectorScreen(
                            onBackClick = { navController.pop() },
                            onAppClick = { selectedApp ->
                                navController.navigate(
                                    Destination.SelectedApplicationInfo(
                                        selectedApp,
                                        destination.patchesSelection,
                                    )
                                )
                            },
                            viewModel = getComposeViewModel {
                                parametersOf(
                                    destination.packageName,
                                    destination.patchesSelection
                                )
                            }
                        )

                        is Destination.SelectedApplicationInfo -> SelectedAppInfoScreen(
                            onPatchClick = { app, patches, options ->
                                navController.navigate(
                                    Destination.Installer(
                                        app, patches, options
                                    )
                                )
                            },
                            onBackClick = navController::pop,
                            vm = getComposeViewModel {
                                parametersOf(
                                    SelectedAppInfoViewModel.Params(
                                        destination.selectedApp,
                                        destination.patchesSelection
                                    )
                                )
                            }
                        )

                        is Destination.Installer -> InstallerScreen(
                            onBackClick = { navController.popUpTo { it is Destination.Dashboard } },
                            vm = getComposeViewModel { parametersOf(destination) }
                        )
                    }
                }
            }
        }
    }
}
