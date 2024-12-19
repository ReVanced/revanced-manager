package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.destination.SettingsDestination
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstalledAppInfoScreen
import app.revanced.manager.ui.screen.PatcherScreen
import app.revanced.manager.ui.screen.SelectedAppInfoScreen
import app.revanced.manager.ui.screen.SettingsScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
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

                EventEffect(vm.appSelectFlow) { app ->
                    navController.navigate(Destination.SelectedApplicationInfo(app))
                }

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onSettingsClick = { navController.navigate(Destination.Settings()) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) },
                            onUpdateClick = {
                                navController.navigate(Destination.Settings(SettingsDestination.Update()))
                            },
                            onDownloaderPluginClick = {
                                navController.navigate(Destination.Settings(SettingsDestination.Downloads))
                            },
                            onAppClick = { installedApp ->
                                navController.navigate(
                                    Destination.InstalledApplicationInfo(
                                        installedApp
                                    )
                                )
                            }
                        )

                        is Destination.InstalledApplicationInfo -> InstalledAppInfoScreen(
                            onPatchClick = vm::selectApp,
                            onBackClick = { navController.pop() },
                            viewModel = getComposeViewModel { parametersOf(destination.installedApp) }
                        )

                        is Destination.Settings -> SettingsScreen(
                            onBackClick = { navController.pop() },
                            startDestination = destination.startDestination
                        )

                        is Destination.AppSelector -> AppSelectorScreen(
                            onSelect = vm::selectApp,
                            onStorageSelect = vm::selectApp,
                            onBackClick = { navController.pop() }
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
