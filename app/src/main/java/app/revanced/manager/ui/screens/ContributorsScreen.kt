package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.Global.Companion.websiteUrl
import app.revanced.manager.R
import app.revanced.manager.ui.components.ContributorsCard
import app.revanced.manager.ui.components.IconHeader
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
        Column(
            Modifier
                .padding(8.dp)
                .height(1400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            IconHeader()

            ContributorsCard(
                stringResource(R.string.cli_contributors),
                data = vm.cliContributorsList,
                size = 100
            )
            ContributorsCard(
                    stringResource(R.string.patcher_contributors),
                    data = vm.patcherContributorsList,
                    size = 100
            )
            ContributorsCard(
                    stringResource(R.string.patches_contributors),
                    data = vm.patchesContributorsList,
                    size = 150
            )
            ContributorsCard(
                    stringResource(R.string.manager_contributors),
                    data = vm.managerContributorsList,
                    size = 100
            )
            ContributorsCard(
                    stringResource(R.string.integrations_contributors),
                    data = vm.integrationsContributorsList,
                    size = 200
            )

            val currentUriHandler = LocalUriHandler.current

            Spacer(Modifier.weight(1f, true))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { currentUriHandler.openUri("${websiteUrl}/github") }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_github_24),
                    contentDescription = "GitHub Link"
                )
                Spacer(Modifier.padding(4.dp))
                Text(text = "GitHub")
            }
        }
}
