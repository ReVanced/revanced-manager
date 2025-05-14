package app.revanced.manager

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.revanced.manager.ui.model.navigation.AppSelector
import app.revanced.manager.ui.model.navigation.ComplexParameter
import app.revanced.manager.ui.model.navigation.Dashboard
import app.revanced.manager.ui.model.navigation.InstalledApplicationInfo
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.ui.model.navigation.SelectedApplicationInfo
import app.revanced.manager.ui.model.navigation.Settings
import app.revanced.manager.ui.model.navigation.Update
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstalledAppInfoScreen
import app.revanced.manager.ui.screen.PatcherScreen
import app.revanced.manager.ui.screen.PatchesSelectorScreen
import app.revanced.manager.ui.screen.RequiredOptionsScreen
import app.revanced.manager.ui.screen.SelectedAppInfoScreen
import app.revanced.manager.ui.screen.SettingsScreen
import app.revanced.manager.ui.screen.UpdateScreen
import app.revanced.manager.ui.screen.settings.AboutSettingsScreen
import app.revanced.manager.ui.screen.settings.AdvancedSettingsScreen
import app.revanced.manager.ui.screen.settings.ContributorSettingsScreen
import app.revanced.manager.ui.screen.settings.DeveloperSettingsScreen
import app.revanced.manager.ui.screen.settings.DownloadsSettingsScreen
import app.revanced.manager.ui.screen.settings.GeneralSettingsScreen
import app.revanced.manager.ui.screen.settings.ImportExportSettingsScreen
import app.revanced.manager.ui.screen.settings.LicensesSettingsScreen
import app.revanced.manager.ui.screen.settings.update.ChangelogsSettingsScreen
import app.revanced.manager.ui.screen.settings.update.UpdatesSettingsScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.compose.navigation.koinNavViewModel
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.getViewModel as getActivityViewModel

class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        installSplashScreen()

        val vm: MainViewModel = getActivityViewModel()

        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                onResult = vm::applyLegacySettings
            )
            val theme by vm.prefs.theme.getAsState()
            val dynamicColor by vm.prefs.dynamicColor.getAsState()

            EventEffect(vm.legacyImportActivityFlow) {
                try {
                    launcher.launch(it)
                } catch (_: ActivityNotFoundException) {
                }
            }

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
        navController.navigateComplex(
            SelectedApplicationInfo,
            SelectedApplicationInfo.ViewModelParams(app)
        )
    }

    NavHost(
        navController = navController,
        startDestination = Dashboard,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        composable<Dashboard> {
            DashboardScreen(
                onSettingsClick = { navController.navigate(Settings) },
                onAppSelectorClick = {
                    navController.navigate(AppSelector)
                },
                onUpdateClick = {
                    navController.navigate(Update())
                },
                onDownloaderPluginClick = {
                    navController.navigate(Settings.Downloads)
                },
                onAppClick = { packageName ->
                    navController.navigate(InstalledApplicationInfo(packageName))
                }
            )
        }

        composable<InstalledApplicationInfo> {
            val data = it.toRoute<InstalledApplicationInfo>()

            InstalledAppInfoScreen(
                onPatchClick = vm::selectApp,
                onBackClick = navController::popBackStack,
                viewModel = koinViewModel { parametersOf(data.packageName) }
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
                vm = koinViewModel { parametersOf(it.getComplexArg<Patcher.ViewModelParams>()) }
            )
        }

        composable<Update> {
            val data = it.toRoute<Update>()

            UpdateScreen(
                onBackClick = navController::popBackStack,
                vm = koinViewModel { parametersOf(data.downloadOnScreenEntry) }
            )
        }

        navigation<SelectedApplicationInfo>(startDestination = SelectedApplicationInfo.Main) {
            composable<SelectedApplicationInfo.Main> {
                val parentBackStackEntry = navController.navGraphEntry(it)
                val data =
                    parentBackStackEntry.getComplexArg<SelectedApplicationInfo.ViewModelParams>()
                val viewModel =
                    koinNavViewModel<SelectedAppInfoViewModel>(viewModelStoreOwner = parentBackStackEntry) {
                        parametersOf(data)
                    }

                SelectedAppInfoScreen(
                    onBackClick = navController::popBackStack,
                    onPatchClick = {
                        it.lifecycleScope.launch {
                            navController.navigateComplex(
                                Patcher,
                                viewModel.getPatcherParams()
                            )
                        }
                    },
                    onPatchSelectorClick = { app, patches, options ->
                        navController.navigateComplex(
                            SelectedApplicationInfo.PatchesSelector,
                            SelectedApplicationInfo.PatchesSelector.ViewModelParams(
                                app,
                                patches,
                                options
                            )
                        )
                    },
                    onRequiredOptions = { app, patches, options ->
                        navController.navigateComplex(
                            SelectedApplicationInfo.RequiredOptions,
                            SelectedApplicationInfo.PatchesSelector.ViewModelParams(
                                app,
                                patches,
                                options
                            )
                        )
                    },
                    vm = viewModel
                )
            }

            composable<SelectedApplicationInfo.PatchesSelector> {
                val data =
                    it.getComplexArg<SelectedApplicationInfo.PatchesSelector.ViewModelParams>()
                val selectedAppInfoVm = koinNavViewModel<SelectedAppInfoViewModel>(
                    viewModelStoreOwner = navController.navGraphEntry(it)
                )

                PatchesSelectorScreen(
                    onBackClick = navController::popBackStack,
                    onSave = { patches, options ->
                        selectedAppInfoVm.updateConfiguration(patches, options)
                        navController.popBackStack()
                    },
                    vm = koinViewModel { parametersOf(data) }
                )
            }

            composable<SelectedApplicationInfo.RequiredOptions> {
                val data =
                    it.getComplexArg<SelectedApplicationInfo.PatchesSelector.ViewModelParams>()
                val selectedAppInfoVm = koinNavViewModel<SelectedAppInfoViewModel>(
                    viewModelStoreOwner = navController.navGraphEntry(it)
                )

                RequiredOptionsScreen(
                    onBackClick = navController::popBackStack,
                    onContinue = { patches, options ->
                        selectedAppInfoVm.updateConfiguration(patches, options)
                        it.lifecycleScope.launch {
                            navController.navigateComplex(
                                Patcher,
                                selectedAppInfoVm.getPatcherParams()
                            )
                        }
                    },
                    vm = koinViewModel { parametersOf(data) }
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

            composable<Settings.Developer> {
                DeveloperSettingsScreen(onBackClick = navController::popBackStack)
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
                ChangelogsSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Contributors> {
                ContributorSettingsScreen(onBackClick = navController::popBackStack)
            }

            composable<Settings.Licenses> {
                LicensesSettingsScreen(onBackClick = navController::popBackStack)
            }

        }
    }
}

@Composable
private fun NavController.navGraphEntry(entry: NavBackStackEntry) =
    remember(entry) { getBackStackEntry(entry.destination.parent!!.id) }

// Androidx Navigation does not support storing complex types in route objects, so we have to store them inside the saved state handle of the back stack entry instead.
private fun <T : Parcelable, R : ComplexParameter<T>> NavController.navigateComplex(
    route: R,
    data: T
) {
    navigate(route)
    getBackStackEntry(route).savedStateHandle["args"] = data
}

private fun <T : Parcelable> NavBackStackEntry.getComplexArg() = savedStateHandle.get<T>("args")!!