package app.revanced.manager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.BottomNavBar
import app.revanced.manager.ui.screens.destinations.*
import com.ramcosta.composedestinations.DestinationsNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            when (navController.appCurrentDestinationAsState().value) {
                PatcherSubscreenDestination -> {
                    AppBar(
                        title = { Text("Patcher") }
                    )
                }
                MoreSubscreenDestination -> {
                    AppBar(
                        title = { Text("More") }
                    )
                }
                AppSelectorScreenDestination -> {
                    AppBar(
                        title = { Text("Select an app") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                fun selectApp(pickerInitialUri: Uri) {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/vnd.android.package-archive"
                                    }
                                    ContextCompat.startActivity(context, intent, null)

                                }
                                selectApp(pickerInitialUri = "file:///storage/emulated/0/".toUri())
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                PatchesSelectorScreenDestination -> {
                    AppBar(
                        title = { Text("Select patches") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                SettingsScreenDestination -> {
                    AppBar(
                        title = { Text(text = "Settings") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                AboutScreenDestination -> {
                    AppBar(
                        title = { Text(text = "About") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Return"
                                )
                            }
                        }
                    )
                }
                ContributorsScreenDestination -> {
                    AppBar(
                        title = { Text(text = "Contributors") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
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
                        title = { Text("ReVanced Manager") }
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
                navController.appCurrentDestinationAsState().value != SettingsScreenDestination
                &&
                navController.appCurrentDestinationAsState().value != AboutScreenDestination
                &&
                navController.appCurrentDestinationAsState().value != ContributorsScreenDestination
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