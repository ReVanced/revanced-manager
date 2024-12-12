package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.revanced.manager.ui.model.navigation.*
import app.revanced.manager.ui.screen.*
import app.revanced.manager.ui.screen.settings.*
import app.revanced.manager.ui.screen.settings.update.ChangelogsScreen
import app.revanced.manager.ui.screen.settings.update.UpdatesSettingsScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.compose.navigation.koinNavViewModel
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
                ReVancedManager(vm)
            }
        }
    }
}

@Composable
private fun ReVancedManager(vm: MainViewModel) {
    val navController = rememberNavController()

    EventEffect(vm.appSelectFlow) { app ->
        // navController.navigate(SelectedApplicationInfo(app))
    }

    NavHost(
        navController = navController,
        startDestination = Dashboard,
    ) {
        composable<Dashboard> {
            DashboardScreen(
                onSettingsClick = { navController.navigate(Settings) },
                onAppSelectorClick = {
                    println("before: ${navController.currentBackStackEntry?.id}")
                    navController.navigate(AppSelector)
                    println("after: ${navController.currentBackStackEntry?.id}")
                },
                onUpdateClick = {
                    navController.navigate(Update())
                    // navController.navigate(Destination.Settings(SettingsDestination.Update()))
                },
                onDownloaderPluginClick = {
                    // navController.navigate(Destination.Settings(SettingsDestination.Downloads))
                    navController.navigate(Settings.Downloads)
                },
                onAppClick = { installedApp ->
                    navController.navigate(InstalledApplicationInfo(installedApp.currentPackageName))
                }
            )
        }

        composable<InstalledApplicationInfo> {
            val data = it.toRoute<InstalledApplicationInfo>()

            InstalledAppInfoScreen(
                onPatchClick = vm::selectApp,
                onBackClick = navController::popBackStack,
                viewModel = getComposeViewModel { parametersOf(data.packageName) }
            )
        }

        composable<AppSelector> {
            AppSelectorScreen(
                onSelect = vm::selectApp,
                onStorageSelect = vm::selectApp,
                onBackClick = navController::popBackStack
            )
        }

        composable<Patcher> {
            PatcherScreen(
                onBackClick = {
                    navController.navigate(route = Dashboard) {
                        launchSingleTop = true
                        popUpTo<Dashboard> {
                            inclusive = false
                        }
                    }
                },
                vm = koinViewModel { parametersOf(it.toRoute<Patcher>()) }
            )
        }

        navigation<SelectedApplicationInfo>(startDestination = SelectedApplicationInfo.Main) {
            composable<SelectedApplicationInfo.Main> {
                val parentBackStackEntry = navController.navGraphEntry(it)
                val data = parentBackStackEntry.toRoute<SelectedApplicationInfo>()

                SelectedAppInfoScreen(
                    onBackClick = navController::popBackStack,
                    onPatchClick = { app, patches, options ->
                        // navController.navigate(Patcher(app, patches, options))
                    },
                    onPatchSelectorClick = { app, patches, options ->
                        /*
                        navController.navigate(
                            SelectedApplicationInfo.PatchesSelector(
                                app,
                                patches,
                                options
                            )
                        )*/
                    },
                    vm = koinNavViewModel<SelectedAppInfoViewModel>(viewModelStoreOwner = parentBackStackEntry) {
                        parametersOf(
                            SelectedAppInfoViewModel.Params(
                                data.selectedApp,
                                data.patchSelection
                            )
                        )
                    }
                )
            }

            composable<SelectedApplicationInfo.PatchesSelector>(
                // typeMap = mapOf(typeOf<SelectedApplicationInfo.PatchesSelector>() to SelectedApplicationInfo.PatchesSelector.navType)
            ) {
                val data = it.toRoute<SelectedApplicationInfo.PatchesSelector>()
                val selectedAppInfoVm = koinNavViewModel<SelectedAppInfoViewModel>(
                    viewModelStoreOwner = navController.navGraphEntry(it)
                )

                PatchesSelectorScreen(
                    onBackClick = navController::popBackStack,
                    onSave = { patches, options ->
                        selectedAppInfoVm.updateConfiguration(patches, options)
                        navController.popBackStack()
                    },
                    vm = koinViewModel {
                        parametersOf(
                            PatchesSelectorViewModel.Params(
                                data.app,
                                data.currentSelection,
                                data.options,
                            )
                        )
                    }
                )
            }
        }

        navigation<Settings>(startDestination = Settings.Main) {
            composable<Settings.Main> {
                SettingsScreen(
                    onBackClick = navController::popBackStack,
                    navigate = navController::navigate
                )
            }

            composable<Settings.General> {
                GeneralSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Advanced> {
                AdvancedSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Updates> {
                UpdatesSettingsScreen(
                    onBackClick = navController::popBackStack,
                    onChangelogClick = { navController.navigate(Settings.Changelogs) },
                    onUpdateClick = { navController.navigate(Update()) }
                )
            }

            composable<Settings.Downloads> {
                DownloadsSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.ImportExport> {
                ImportExportSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.About> {
                AboutSettingsScreen(
                    onBackClick = navController::popBackStack,
                    navigate = navController::navigate
                )
            }

            composable<Settings.Changelogs> {
                ChangelogsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Contributors> {
                ContributorScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Licenses> {
                LicensesScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.DeveloperOptions> {
                DeveloperOptionsScreen(onBackClick = navController::popBackStack)
            }
        }
    }
}

@Composable
private fun NavController.navGraphEntry(entry: NavBackStackEntry) = remember(entry) {
    getBackStackEntry(entry.destination.parent!!)
}

/*
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
 */