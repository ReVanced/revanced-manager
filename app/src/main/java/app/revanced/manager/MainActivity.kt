package app.revanced.manager

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.ui.component.AutoUpdatesDialog
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.screen.AppInfoScreen
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstallerScreen
import app.revanced.manager.ui.screen.PatchesSelectorScreen
import app.revanced.manager.ui.screen.SettingsScreen
import app.revanced.manager.ui.screen.VersionSelectorScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.getViewModel as getActivityViewModel

class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val vm: MainViewModel = getActivityViewModel()

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

                if (firstLaunch) {
                    var legacyActivityState by rememberSaveable { mutableStateOf(LegacyActivity.NOT_LAUNCHED) }
                    if (legacyActivityState == LegacyActivity.NOT_LAUNCHED) {
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result: ActivityResult ->
                            if (result.resultCode == RESULT_OK) {
                                if (result.data != null) {
                                    val jsonData = result.data!!.getStringExtra("data")!!
                                    vm.applyLegacySettings(jsonData)
                                }
                            } else {
                                legacyActivityState = LegacyActivity.FAILED
                                toast(getString(R.string.legacy_import_failed))
                            }
                        }

                        val intent = Intent().apply {
                            setClassName(
                                "app.revanced.manager.flutter",
                                "app.revanced.manager.flutter.ExportSettingsActivity"
                            )
                        }

                        LaunchedEffect(Unit) {
                            try {
                                launcher.launch(intent)
                            } catch (e: Exception) {
                                if (e !is ActivityNotFoundException) {
                                    toast(getString(R.string.legacy_import_failed))
                                    Log.e(tag, "Failed to launch legacy import activity: $e")
                                }
                                legacyActivityState = LegacyActivity.FAILED
                            }
                        }

                        legacyActivityState = LegacyActivity.LAUNCHED
                    } else if (legacyActivityState == LegacyActivity.FAILED){
                        AutoUpdatesDialog(vm::applyAutoUpdatePrefs)
                    }
                }

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onSettingsClick = { navController.navigate(Destination.Settings) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) },
                            onAppClick = { installedApp -> navController.navigate(Destination.ApplicationInfo(installedApp)) }
                        )

                        is Destination.ApplicationInfo -> AppInfoScreen(
                            onPatchClick = { packageName, patchesSelection ->
                                navController.navigate(Destination.VersionSelector(packageName, patchesSelection))
                            },
                            onBackClick = { navController.pop() },
                            viewModel = getViewModel { parametersOf(destination.installedApp) }
                        )

                        is Destination.Settings -> SettingsScreen(
                            onBackClick = { navController.pop() }
                        )

                        is Destination.AppSelector -> AppSelectorScreen(
                            onAppClick = { navController.navigate(Destination.VersionSelector(it)) },
                            onStorageClick = { navController.navigate(Destination.PatchesSelector(it)) },
                            onBackClick = { navController.pop() }
                        )

                        is Destination.VersionSelector -> VersionSelectorScreen(
                            onBackClick = { navController.pop() },
                            onAppClick = { selectedApp ->
                                navController.navigate(
                                    Destination.PatchesSelector(
                                        selectedApp,
                                        destination.patchesSelection
                                    )
                                )
                            },
                            viewModel = getViewModel { parametersOf(destination.packageName, destination.patchesSelection) }
                        )

                        is Destination.PatchesSelector -> PatchesSelectorScreen(
                            onBackClick = { navController.pop() },
                            onPatchClick = { patches, options ->
                                navController.navigate(
                                    Destination.Installer(
                                        destination.selectedApp,
                                        patches,
                                        options
                                    )
                                )
                            },
                            vm = getViewModel { parametersOf(destination) }
                        )

                        is Destination.Installer -> InstallerScreen(
                            onBackClick = { navController.popUpTo { it is Destination.Dashboard } },
                            vm = getViewModel { parametersOf(destination) }
                        )
                    }
                }
            }
        }
    }

    private enum class LegacyActivity {
        NOT_LAUNCHED,
        LAUNCHED,
        FAILED
    }
}
