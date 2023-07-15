package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.screen.AppDownloaderScreen
import app.revanced.manager.ui.screen.AppSelectorScreen
import app.revanced.manager.ui.screen.DashboardScreen
import app.revanced.manager.ui.screen.InstallerScreen
import app.revanced.manager.ui.screen.PatchesSelectorScreen
import app.revanced.manager.ui.screen.SettingsScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.MainViewModel
import coil.Coil
import coil.ImageLoader
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.koin.android.ext.android.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt
import org.koin.androidx.viewmodel.ext.android.getViewModel as getActivityViewModel

class MainActivity : ComponentActivity() {
    private val prefs: PreferencesManager = get()

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        getActivityViewModel<MainViewModel>()

        val scale = this.resources.displayMetrics.density
        val pixels = (36 * scale).roundToInt()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(pixels, true, this@MainActivity))
                }
                .build()
        )

        setContent {
            val theme by prefs.theme.getAsState()
            val dynamicColor by prefs.dynamicColor.getAsState()

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
                            onSettingsClick = { navController.navigate(Destination.Settings) },
                            onAppSelectorClick = { navController.navigate(Destination.AppSelector) }
                        )

                        is Destination.Settings -> SettingsScreen(
                            onBackClick = { navController.pop() }
                        )

                        is Destination.AppSelector -> AppSelectorScreen(
                            onAppClick = { navController.navigate(Destination.PatchesSelector(it)) },
                            onDownloaderClick = { navController.navigate(Destination.AppDownloader(it)) },
                            onBackClick = { navController.pop() }
                        )

                        is Destination.AppDownloader -> AppDownloaderScreen(
                            onBackClick = { navController.pop() },
                            onApkClick = { navController.navigate(Destination.PatchesSelector(it)) },
                            viewModel = getViewModel { parametersOf(destination.app) }
                        )

                        is Destination.PatchesSelector -> PatchesSelectorScreen(
                            onBackClick = { navController.popUpTo { it is Destination.AppSelector } },
                            onPatchClick = { patches, options ->
                                navController.navigate(
                                    Destination.Installer(
                                        destination.input,
                                        patches,
                                        options
                                    )
                                )
                            },
                            vm = getViewModel { parametersOf(destination.input) }
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
}