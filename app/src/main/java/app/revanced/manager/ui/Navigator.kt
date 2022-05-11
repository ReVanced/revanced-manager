package app.revanced.manager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import app.revanced.manager.ui.screens.mainsubscreens.FeedSubscreen
import app.revanced.manager.ui.screens.mainsubscreens.PatcherSubscreen

//@Composable
//fun NavigatorContainer() {
//    var screenName by rememberSaveable { mutableStateOf("Dashboard") }
//
//    when (screenName) {
//        "Dashboard" -> {
//            FeedSubscreen(onNavigateClick = { screenName = "screen2" })
//        }
//        "Patcher" -> {
//            PatcherSubscreen(onNavigateClick = { screenName = "screen1" })
//        }
//    }
//}