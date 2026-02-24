package app.revanced.manager

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.revanced.manager.ui.model.navigation.Announcement
import app.revanced.manager.ui.model.navigation.Announcements
import app.revanced.manager.ui.model.navigation.AppSelector
import app.revanced.manager.ui.model.navigation.BundleInformation
import app.revanced.manager.ui.model.navigation.ComplexParameter
import app.revanced.manager.ui.model.navigation.Dashboard
import app.revanced.manager.ui.model.navigation.InstalledApplicationInfo
import app.revanced.manager.ui.model.navigation.Onboarding
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.ui.model.navigation.SelectedApplicationInfo
import app.revanced.manager.ui.model.navigation.Settings
import app.revanced.manager.ui.model.navigation.Update
import app.revanced.manager.ui.screen.AnnouncementScreen
import app.revanced.manager.ui.screen.AnnouncementsScreen
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.BundleInformationScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstalledAppInfoScreen
import app.revanced.manager.ui.screen.OnboardingScreen
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
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.deepLinkedComposable
import app.revanced.manager.util.navigateSafe
import app.revanced.manager.util.popBackStackSafe
import app.revanced.manager.util.resetListItemColorsCached
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.getViewModel as getActivityViewModel


class MainActivity : AppCompatActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        installSplashScreen()

        val vm: MainViewModel = getActivityViewModel()

        setContent {
            val theme by vm.prefs.theme.getAsState()
            val dynamicColor by vm.prefs.dynamicColor.getAsState()
            val pureBlackTheme by vm.prefs.pureBlackTheme.getAsState()

            LaunchedEffect(theme, dynamicColor, pureBlackTheme) {
                resetListItemColorsCached()
            }

            ReVancedManagerTheme(
                darkTheme = theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK,
                dynamicColor = dynamicColor,
                pureBlackTheme = pureBlackTheme
            ) {
                ReVancedManager(vm)
            }
        }
    }
}

@Composable
private fun ReVancedManager(vm: MainViewModel) {
    val navController = rememberNavController()
    val dashboardVm: DashboardViewModel = koinViewModel()
    val completedOnboarding by vm.prefs.completedOnboarding.getAsState()

    EventEffect(vm.appSelectFlow) { app ->
        navController.navigateComplex(
            SelectedApplicationInfo,
            SelectedApplicationInfo.ViewModelParams(app)
        )
    }

    NavHost(
        navController = navController,
        startDestination = if (completedOnboarding) Dashboard else Onboarding,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        composable<Onboarding> {
            OnboardingScreen(
                onFinish = { navController.navigateSafe(Dashboard) },
                onAppClick = vm::selectApp,
            )
        }

        composable<Dashboard> {
            DashboardScreen(
                vm = dashboardVm,
                onSettingsClick = { navController.navigateSafe(Settings) },
                onAppSelectorClick = {
                    navController.navigateSafe(AppSelector)
                },
                onUpdateClick = {
                    navController.navigateSafe(Update())
                },
                onDownloaderClick = {
                    navController.navigateSafe(Settings.Downloads)
                },
                onAppClick = { packageName ->
                    navController.navigateSafe(InstalledApplicationInfo(packageName))
                },
                onBundleClick = { uid ->
                    navController.navigateSafe(BundleInformation(uid))
                },
                onAnnouncementsClick = {
                    navController.navigate(Announcements)
                },
                onAnnouncementClick = { announcement ->
                    navController.navigateComplex(Announcement, announcement)
                }
            )
        }

        composable<BundleInformation> {
            BundleInformationScreen(
                onBackClick = navController::popBackStackSafe,
                viewModel = koinViewModel()
            )
        }

        composable<InstalledApplicationInfo> {
            val data = it.toRoute<InstalledApplicationInfo>()

            InstalledAppInfoScreen(
                onPatchClick = vm::selectApp,
                onBackClick = navController::popBackStackSafe,
                viewModel = koinViewModel { parametersOf(data.packageName) }
            )
        }

        composable<AppSelector> {
            AppSelectorScreen(
                onSelect = vm::selectApp,
                onStorageSelect = vm::selectApp,
                onBackClick = navController::popBackStackSafe
            )
        }

        composable<Patcher> {
            PatcherScreen(
                onBackClick = {
                    navController.navigateSafe(route = Dashboard) {
                        launchSingleTop = true
                        popUpTo<Dashboard> {
                            inclusive = false
                        }
                    }
                },
                viewModel = koinViewModel { parametersOf(it.getComplexArg<Patcher.ViewModelParams>()) }
            )
        }

        composable<Update> {
            val data = it.toRoute<Update>()

            UpdateScreen(
                onBackClick = navController::popBackStackSafe,
                vm = koinViewModel { parametersOf(data.downloadOnScreenEntry) }
            )
        }

        composable<Announcements> {
            AnnouncementsScreen(
                onBackClick = navController::popBackStack,
                onAnnouncementClick = { announcement ->
                    navController.navigateComplex(Announcement, announcement)
                }
            )
        }

        composable<Announcement> {
            AnnouncementScreen(
                onBackClick = navController::popBackStack,
                announcement = it.getComplexArg()
            )
        }

        navigation<SelectedApplicationInfo>(startDestination = SelectedApplicationInfo.Main) {
            composable<SelectedApplicationInfo.Main> {
                val parentBackStackEntry = navController.navGraphEntry(it)
                val data =
                    parentBackStackEntry.getComplexArg<SelectedApplicationInfo.ViewModelParams>()
                val viewModel =
                    koinViewModel<SelectedAppInfoViewModel>(viewModelStoreOwner = parentBackStackEntry) {
                        parametersOf(data)
                    }

                SelectedAppInfoScreen(
                    onBackClick = navController::popBackStackSafe,
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
                val selectedAppInfoVm = koinViewModel<SelectedAppInfoViewModel>(
                    viewModelStoreOwner = navController.navGraphEntry(it)
                )

                PatchesSelectorScreen(
                    onBackClick = navController::popBackStackSafe,
                    onSave = { patches, options ->
                        selectedAppInfoVm.updateConfiguration(patches, options)
                        navController.popBackStackSafe()
                    },
                    viewModel = koinViewModel { parametersOf(data) }
                )
            }

            composable<SelectedApplicationInfo.RequiredOptions> {
                val data =
                    it.getComplexArg<SelectedApplicationInfo.PatchesSelector.ViewModelParams>()
                val selectedAppInfoVm = koinViewModel<SelectedAppInfoViewModel>(
                    viewModelStoreOwner = navController.navGraphEntry(it)
                )

                RequiredOptionsScreen(
                    onBackClick = navController::popBackStackSafe,
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
            deepLinkedComposable<Settings.Main>("settings") {
                SettingsScreen(
                    onBackClick = navController::popBackStackSafe,
                    navigate = navController::navigateSafe
                )
            }

            deepLinkedComposable<Settings.General>("settings/general") {
                GeneralSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            deepLinkedComposable<Settings.Advanced>("settings/advanced") {
                AdvancedSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            deepLinkedComposable<Settings.Developer>("settings/developer") {
                DeveloperSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            deepLinkedComposable<Settings.Updates>("settings/updates") {
                UpdatesSettingsScreen(
                    onBackClick = navController::popBackStackSafe,
                    onChangelogClick = { navController.navigateSafe(Settings.Changelogs) },
                    onUpdateClick = { navController.navigateSafe(Update()) }
                )
            }

            deepLinkedComposable<Settings.Downloads>("settings/downloads") {
                DownloadsSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            deepLinkedComposable<Settings.ImportExport>("settings/import-export") {
                ImportExportSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            deepLinkedComposable<Settings.About>("about") {
                AboutSettingsScreen(
                    onBackClick = navController::popBackStackSafe,
                    navigate = navController::navigateSafe
                )
            }

            composable<Settings.Changelogs> {
                ChangelogsSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            composable<Settings.Contributors> {
                ContributorSettingsScreen(onBackClick = navController::popBackStackSafe)
            }

            composable<Settings.Licenses> {
                LicensesSettingsScreen(onBackClick = navController::popBackStackSafe)
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
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
        getBackStackEntry(route).savedStateHandle["args"] = data
    }
}

private fun <T : Parcelable> NavBackStackEntry.getComplexArg() = savedStateHandle.get<T>("args")!!