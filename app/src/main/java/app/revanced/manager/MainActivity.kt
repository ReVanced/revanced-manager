package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.destination.SettingsDestination
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstalledAppInfoScreen
import app.revanced.manager.ui.screen.PatcherScreen
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
import org.koin.androidx.compose.koinViewModel as getComposeViewModel
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

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onSettingsClick = { navController.navigate(Destination.Settings()) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) },
                            onUpdateClick = { navController.navigate(
                                Destination.Settings(SettingsDestination.Update())
                            ) },
                            onAppClick = { installedApp ->
                                navController.navigate(
                                    Destination.InstalledApplicationInfo(
                                        installedApp
                                    )
                                )
                            }
                        )

                        is Destination.InstalledApplicationInfo -> InstalledAppInfoScreen(
                            onPatchClick = { packageName, patchSelection ->
                                navController.navigate(
                                    Destination.VersionSelector(
                                        packageName,
                                        patchSelection
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
                                        destination.patchSelection,
                                    )
                                )
                            },
                            viewModel = getComposeViewModel {
                                parametersOf(
                                    destination.packageName,
                                    destination.patchSelection
                                )
                            }
                        )

                        is Destination.SelectedApplicationInfo -> SelectedAppInfoScreen(
                            onPatchClick = { app, patches, options ->
                                navController.navigate(
                                    Destination.Patcher(
                                        app, patches, options
                                    )
                                )
                            },
                            onBackClick = navController::pop,
                            vm = getComposeViewModel {
                                parametersOf(
                                    SelectedAppInfoViewModel.Params(
                                        destination.selectedApp,
                                        destination.patchSelection
                                    )
                                )
                            }
                        )

                        is Destination.Patcher -> PatcherScreen(
                            onBackClick = { navController.popUpTo { it is Destination.Dashboard } },
                            vm = getComposeViewModel { parametersOf(destination) }
                        )
                    }
                }
            }
        }
    }
}
