package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.BottomNavBar
import app.revanced.manager.ui.screens.destinations.DashboardSubscreenDestination
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.utils.startDestination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = true)
@Composable
fun MainScreen() {
    val navControl = rememberNavController()
    Scaffold(
        topBar = {AppBar() },
        bottomBar = {BottomNavBar(navControl)},
        content = { innerPadding ->
            DestinationsNavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navControl,
                navGraph = NavGraphs.root,

                startRoute = DashboardSubscreenDestination.startDestination
            )
        }
    )
}