package app.revanced.manager.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.revanced.manager.compose.destination.Destination
import app.revanced.manager.compose.ui.theme.ReVancedManagerTheme
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.rememberNavController

class MainActivity : ComponentActivity() {

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        setContent {
            ReVancedManagerTheme(
                darkTheme = true, // TODO: Implement preferences
                dynamicColor = false
            ) {
                val navController = rememberNavController<Destination>(startDestination = Destination.Home)

                NavBackHandler(navController)

                AnimatedNavHost(
                    controller = navController,
                ) { destination ->
                    when (destination) {
                        Destination.Home -> {}  // TODO: Add screens
                    }
                }
            }
        }
    }
}