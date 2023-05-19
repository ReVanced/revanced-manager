package app.revanced.manager.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.compose.domain.manager.PreferencesManager
import app.revanced.manager.compose.ui.destination.Destination
import app.revanced.manager.compose.ui.screen.*
import app.revanced.manager.compose.ui.theme.ReVancedManagerTheme
import app.revanced.manager.compose.ui.theme.Theme
import app.revanced.manager.compose.util.PM
import dev.olshevski.navigation.reimagined.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    private val prefs: PreferencesManager by inject()
    private val mainScope = MainScope()

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val context = this
        mainScope.launch(Dispatchers.IO) {
            PM.loadApps(context)
        }

        setContent {
            ReVancedManagerTheme(
                darkTheme = prefs.theme == Theme.SYSTEM && isSystemInDarkTheme() || prefs.theme == Theme.DARK,
                dynamicColor = prefs.dynamicColor
            ) {
                val navController = rememberNavController<Destination>(startDestination = Destination.Dashboard)

                NavBackHandler(navController)

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onSettingsClick = { navController.navigate(Destination.Settings) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) }
                        )

                        is Destination.Settings -> SettingsScreen(
                            onBackClick = { navController.pop() }
                        )

                        is Destination.AppSelector -> AppSelectorScreen(
                            onAppClick = { navController.navigate(Destination.PatchesSelector(it)) },
                            onBackClick = { navController.pop() }
                        )

                        is Destination.PatchesSelector -> PatchesSelectorScreen(
                            onBackClick = { navController.pop() },
                            startPatching = {
                                navController.navigate(
                                    Destination.Installer(
                                        destination.input,
                                        it
                                    )
                                )
                            },
                            vm = getViewModel { parametersOf(destination.input) }
                        )

                        is Destination.Installer -> InstallerScreen(getViewModel {
                            parametersOf(
                                destination.input,
                                destination.selectedPatches
                            )
                        })
                    }
                }
            }
        }
    }
}