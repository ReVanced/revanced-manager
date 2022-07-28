package app.revanced.manager.ui.screens

import android.app.Application
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.BottomNavBar
import app.revanced.manager.ui.screens.destinations.*
import app.revanced.manager.ui.screens.destinations.SettingsTempScreenDestination
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import com.ramcosta.composedestinations.DestinationsNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pvm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity),
    vm: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val getContent =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            // val input = vm.getInputStream(uri) // example
            pvm.setSelectedAppPackage(uri.path!!) // TODO: this is obviously not going to work.
            // TODO: switch over from "selected app package" string to a SelectedApp object.
            // which includes the InputStreamSupplier (or path?), package name and version.
        }

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
                        navigationIcon = { ReturnButton(navController) },
                        actions = {
                            IconButton(onClick = {
                                getContent.launch("application/vnd.android.package-archive")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "From filesystem"
                                )
                            }
                        }
                    )
                }
                PatchesSelectorScreenDestination -> {
                    AppBar(
                        title = { Text("Select patches") },
                        navigationIcon = { ReturnButton(navController) }
                    )
                }
                SettingsScreenDestination -> {
                    AppBar(
                        title = { Text(text = "Settings") },
                        navigationIcon = { ReturnButton(navController) }
                    )
                }
                SettingsTempScreenDestination -> {
                    AppBar(
                        title = { Text(text = "Settings TEMP") },
                        navigationIcon = { ReturnButton(navController) }
                    )
                }
                AboutScreenDestination -> {
                    AppBar(
                        title = { Text(text = "About") },
                        navigationIcon = { ReturnButton(navController) }
                    )
                }
                ContributorsScreenDestination -> {
                    AppBar(
                        title = { Text(text = "Contributors") },
                        navigationIcon = { ReturnButton(navController) }
                    )
                }
                else -> {
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
                navController.appCurrentDestinationAsState().value != SettingsTempScreenDestination
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

@Composable
fun ReturnButton(navController: NavHostController) {
    IconButton(onClick = navController::navigateUp) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Return"
        )
    }
}

class MainViewModel(val app: Application) : AndroidViewModel(app) {
    fun getInputStream(uri: Uri) = app.contentResolver.openInputStream(uri)!!
}