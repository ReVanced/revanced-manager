package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.screens.mainsubscreens.FeedSubscreen

@OptIn(ExperimentalMaterial3Api::class)
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