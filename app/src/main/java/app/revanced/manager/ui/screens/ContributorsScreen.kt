package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    Column(Modifier.padding(8.dp)) {
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
        Divider(Modifier.alpha(.5f))

        Column() {
            ExpandableCard(stringResource(R.string.cli_contributors), data = vm.cliContributorsList, size = 100)
            ExpandableCard(stringResource(R.string.patcher_contributors), data = vm.patcherContributorsList, size = 100)
            ExpandableCard(stringResource(R.string.patches_contributors), data = vm.patchesContributorsList, size = 150)
            ExpandableCard(stringResource(R.string.manager_contributors), data = vm.managerContributorsList, size = 100)
            ExpandableCard(stringResource(R.string.integrations_contrubutors), data = vm.integrationsContributorsList, size = 200)
        }

    }
}
