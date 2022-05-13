package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.models.DashboardViewModel
import app.revanced.manager.ui.screens.NavGraphs
import app.revanced.manager.ui.screens.destinations.AppSelectorScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.popUpTo

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
@RootNavGraph
fun DashboardSubscreen(
    navigator: NavController,
    vm: DashboardViewModel = DashboardViewModel()
) {
    LaunchedEffect(Unit) { vm.fetchLastCommit() }

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.card_announcement_header),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(id = R.string.card_announcement_body_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(0.dp, 8.dp)
                )
            }
        }
        Row(modifier = Modifier.sizeIn(maxHeight = 200.dp)) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .sizeIn(minWidth = 200.dp, maxWidth = 200.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_commits_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(modifier = Modifier.padding(0.dp, 8.dp)) {
                        Text(
                            text = stringResource(id = R.string.card_commits_body_patcher),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = vm.patcherCommitDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.padding(0.dp, 8.dp)) {
                        Text(
                            text = stringResource(id = R.string.card_commits_body_manager),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = vm.managerCommitDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Card(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .padding(4.dp)
                    .sizeIn(minWidth = 250.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_credits_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.card_credits_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
        }
        Button(onClick = {
            navigator.navigate(
                AppSelectorScreenDestination(
                    "lesss goooo",
                    arrayOf("aboba")
                ).route
            ) {
                popUpTo(NavGraphs.root)
            }
        }, content = {
            Text("Sus")
        })
    }
}