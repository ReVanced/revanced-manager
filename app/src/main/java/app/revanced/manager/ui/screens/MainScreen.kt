package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.screens.mainsubscreens.FeedSubscreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            AppBar()
        },
        bottomBar = {
            BottomNavBar()
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                FeedSubscreen()
            }
        }
    )
}