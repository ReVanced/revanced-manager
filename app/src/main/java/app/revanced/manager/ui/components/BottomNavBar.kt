package app.revanced.manager.ui.components

import androidx.compose.animation.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.revanced.manager.ui.components.placeholders.Icon
import app.revanced.manager.ui.screens.MainScreenDestinations
import app.revanced.manager.ui.screens.NavGraphs
import app.revanced.manager.ui.screens.appCurrentDestinationAsState
import app.revanced.manager.ui.screens.destinations.AppSelectorScreenDestination
import app.revanced.manager.ui.screens.destinations.PatcherSubscreenDestination
import app.revanced.manager.ui.screens.destinations.TypedDestination
import app.revanced.manager.ui.screens.startAppDestination

@Composable
fun BottomNavBar(
    navigationController: NavController
) {
    val currentDestination: TypedDestination<*> =
        (navigationController.appCurrentDestinationAsState().value
            ?: NavGraphs.root.startAppDestination)
    AnimatedVisibility(
        visible = when (currentDestination) {
            AppSelectorScreenDestination -> false
            PatcherSubscreenDestination -> false
            else -> true
        },
        enter = expandVertically(expandFrom = Alignment.CenterVertically) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.CenterVertically) + fadeOut()
    ) {
        NavigationBar {
            MainScreenDestinations.values().forEach { destination ->
                NavigationBarItem(
                    icon = { Icon(destination.icon, contentDescription = null) }, //wtf is this lmao
                    label = { Text(stringResource(id = destination.label)) },
                    alwaysShowLabel = true,
                    selected = currentDestination.route == destination.direction.route,
                    onClick = {
                        if (destination.direction.route != currentDestination.route) {
                            navigationController.navigate(destination.direction.route) {
                                popUpTo(navigationController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}