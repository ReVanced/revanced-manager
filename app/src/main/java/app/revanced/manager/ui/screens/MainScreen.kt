package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import app.revanced.manager.R
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.BottomNavBar
import app.revanced.manager.ui.screens.mainsubscreens.DashboardSubscreen
import app.revanced.manager.ui.screens.mainsubscreens.PatcherSubscreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var mainScreenName by rememberSaveable {
        mutableStateOf("Dashboard")
    }
    val items = mapOf(
        "Dashboard" to R.drawable.ic_baseline_dashboard_24,
        "Patcher" to R.drawable.ic_baseline_build_24
    )
    Scaffold(
        topBar = {
            AppBar()
        },
        bottomBar = {
            BottomNavBar(screenName = mainScreenName, items, onNavigateClick = {screenName ->
                mainScreenName = screenName
            })
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                when (mainScreenName) {
                    "Dashboard" -> {
                        DashboardSubscreen()
                    }
                    "Patcher" -> {
                        PatcherSubscreen()
                    }
                }
            }
        }
    )
}