package app.revanced.manager.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.revanced.manager.ui.navigation.AppDestination
import app.revanced.manager.ui.navigation.DashboardDestination
import com.xinto.taxi.BackstackNavigator
import com.xinto.taxi.Taxi
import com.xinto.taxi.rememberNavigator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainRootScreen(navigator: BackstackNavigator<AppDestination>) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec = rememberSplineBasedDecay(),
        state = rememberTopAppBarState()
    )
    val mainRootNavigator = rememberNavigator(DashboardDestination.DASHBOARD)
    val currentDestination = mainRootNavigator.currentDestination

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        text = stringResource(mainRootNavigator.currentDestination.label),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                DashboardDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        icon = { Icon(destination.icon, stringResource(destination.label)) },
                        label = { Text(stringResource(destination.label)) },
                        onClick = { mainRootNavigator.replace(destination) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier.padding(paddingValues)
        ) {
            Taxi(
                modifier = Modifier.weight(1f, true),
                navigator = mainRootNavigator,
                transitionSpec = { fadeIn() with fadeOut() }
            ) { destination ->
                when (destination) {
                    DashboardDestination.DASHBOARD -> DashboardScreen()
                    DashboardDestination.PATCHER -> DashboardScreen()
                    DashboardDestination.SETTINGS -> SettingsScreen()
                }
            }
        }

    }
}