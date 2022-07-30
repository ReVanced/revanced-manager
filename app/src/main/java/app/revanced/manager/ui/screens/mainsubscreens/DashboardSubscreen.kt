package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.models.DashboardViewModel
import app.revanced.manager.ui.screens.destinations.ContributorsScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
@RootNavGraph(start = true)
fun DashboardSubscreen(
    navigator: NavController,
    vm: DashboardViewModel = viewModel()
) {
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
                    .weight(1f)
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
                onClick = { navigator.navigate(ContributorsScreenDestination().route) },
                modifier = Modifier
                    .padding(4.dp)
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_contributors_header),
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

    }
}