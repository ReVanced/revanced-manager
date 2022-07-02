package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.R
import app.revanced.manager.ui.components.ExpandableCard
import app.revanced.manager.ui.models.ContributorsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

private const val tag = "ContributorsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun ContributorsScreen(
//    navigator: NavController,
    vm: ContributorsViewModel = viewModel()
) {
//    Box(
//        Modifier
//            .verticalScroll(rememberScrollState())
//            .height(1400.dp)
//            ) {
//        Column(
//            Modifier
//                .padding(8.dp)
//                .height(1400.dp)) {
//            Box() {
//                Icon(
//                    painterResource(id = R.drawable.ic_revanced),
//                    contentDescription = "Header Icon",
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .align(Alignment.TopCenter)
//                        .padding(32.dp)
//                        .size(100.dp),
//                    tint = MaterialTheme.colorScheme.primary
//                )
//            }
//            Divider(Modifier.alpha(.5f))
//
//                ExpandableCard(
//                    stringResource(R.string.cli_contributors),
//                    data = vm.cliContributorsList,
//                    size = 100
//                )
//                ExpandableCard(
//                    stringResource(R.string.patcher_contributors),
//                    data = vm.patcherContributorsList,
//                    size = 100
//                )
//                ExpandableCard(
//                    stringResource(R.string.patches_contributors),
//                    data = vm.patchesContributorsList,
//                    size = 150
//                )
//                ExpandableCard(
//                    stringResource(R.string.manager_contributors),
//                    data = vm.managerContributorsList,
//                    size = 100
//                )
//                ExpandableCard(
//                    stringResource(R.string.integrations_contributors),
//                    data = vm.integrationsContributorsList,
//                    size = 200
//                )
//
//
//        }
//    }
    Box(Modifier.height(1500.dp)) {
        LazyColumn(
            Modifier.height(1200.dp), 
            contentPadding = PaddingValues(4.dp) 
        ) {
            item {
                Box() {
                    Icon(
                        painterResource(id = R.drawable.ic_revanced),
                        contentDescription = "Header Icon",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(32.dp)
                            .size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item {
                Divider(Modifier.alpha(.5f))
            }
            item {
                Box(Modifier.padding(12.dp).height(250.dp)) {
                    ExpandableCard(
                        stringResource(R.string.cli_contributors),
                        data = vm.cliContributorsList,
                        size = 100
                    )
                }
//                Text(text = "hello")
            }
            item {
//                ExpandableCard(
//                    stringResource(R.string.cli_contributors),
//                    data = vm.cliContributorsList,
//                    size = 100
//                )
                Text(text = "hello")

            }
        }
    }
}
