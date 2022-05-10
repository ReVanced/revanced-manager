package app.revanced.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.ui.components.MainScreen
import app.revanced.manager.ui.screens.AppSelectorScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReVancedManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainNavController = rememberNavController()
                    NavHost(navController = mainNavController, startDestination = "mainScreen") {
                        composable("mainScreen") { MainScreen() }
                        composable("appSelectorScreen") { AppSelectorScreen() }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FullPreview() {
    ReVancedManagerTheme {
        MainScreen()
    }
}