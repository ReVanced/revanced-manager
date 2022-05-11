package app.revanced.manager.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import app.revanced.manager.ui.NavGraphs
import app.revanced.manager.ui.appCurrentDestinationAsState
import app.revanced.manager.ui.destinations.TypedDestination
import app.revanced.manager.ui.screens.MainScreenDestinations
import app.revanced.manager.ui.startAppDestination
import com.ramcosta.composedestinations.annotation.Destination

@Composable
fun BottomNavBar(
    navigationController: NavController
) {
    val currentDestination: TypedDestination<*> = (navigationController.appCurrentDestinationAsState().value
        ?: NavGraphs.root.startAppDestination)

    NavigationBar {
        MainScreenDestinations.values().forEach { destination ->
            NavigationBarItem(
                icon = { Icon(destination.icon, contentDescription = null) }, //wtf is this lmao
                label = { Text(stringResource(id = destination.label)) },
                alwaysShowLabel = false,
                selected = currentDestination.route == destination.direction.route ,
                onClick = {
                    if(destination.direction.route != currentDestination.route) {
                        navigationController.navigate(destination.direction.route)
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun BottomNavBarPreview() {
    //BottomNavBar()
}