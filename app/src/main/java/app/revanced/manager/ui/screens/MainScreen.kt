package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.R
import app.revanced.manager.backend.utils.openDiscord
import app.revanced.manager.backend.utils.openGitHub
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.BottomNavBar
import app.revanced.manager.ui.components.placeholders.Icon
import app.revanced.manager.ui.screens.destinations.AppSelectorScreenDestination
import app.revanced.manager.ui.screens.destinations.PatchesSelectorScreenDestination
import app.revanced.manager.ui.screens.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.DestinationsNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            when (navController.appCurrentDestinationAsState().value) {
                AppSelectorScreenDestination -> {
                    AppBar(
                        title = { Text("Select an app...") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                PatchesSelectorScreenDestination -> {
                    AppBar(
                        title = { Text("Select patches...") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                AboutScreenDestination -> {
                    AppBar(
                        title = { Text(text = "About")},
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                else -> {
                    val currentUriHandler = LocalUriHandler.current

                    AppBar(
                        title = { Text("ReVanced Manager") },
                        actions = {
                            IconButton(onClick = { openDiscord(currentUriHandler) }) {
                                Icon(
                                    resourceId = R.drawable.ic_discord_24,
                                    contentDescription = "Discord"
                                )
                            }
                            IconButton(onClick = { openGitHub(currentUriHandler) }) {
                                Icon(
                                    resourceId = R.drawable.ic_github_24,
                                    contentDescription = "GitHub"
                                )
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            // TODO: find a better way to handle such stuff
            if (
                navController.appCurrentDestinationAsState().value != AppSelectorScreenDestination
                &&
                navController.appCurrentDestinationAsState().value != PatchesSelectorScreenDestination
                &&
                        navController.appCurrentDestinationAsState().value != AboutScreenDestination
            ) BottomNavBar(navController)
        },
        content = { innerPadding ->
            DestinationsNavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                navGraph = NavGraphs.root,
            )
        }
    )
}