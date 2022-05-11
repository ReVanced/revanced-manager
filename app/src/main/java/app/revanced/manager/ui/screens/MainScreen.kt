package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.ui.NavGraphs
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun MainScreen() {
    val navControl = rememberNavController()

    Scaffold(
        topBar = {
            AppBar()
        },
        bottomBar = {
            BottomNavBar(navControl)
        },
        content = { innerPadding ->
            DestinationsNavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navControl,
                navGraph = NavGraphs.main

            )
            //            Column(modifier = Modifier.padding(innerPadding)) {
//                when (mainScreenName) {
//                    "Dashboard" -> {
//                        DashboardSubscreen()
//                    }
//                    "Patcher" -> {
//                        PatcherSubscreen()
//                    }
//                }
//            }
        }
    )
}