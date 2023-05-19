package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.compose.ui.component.AppScaffold
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.viewmodel.InstallerScreenViewModel
import app.revanced.manager.compose.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    vm: InstallerScreenViewModel
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.installer),
                onBackClick = { },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            vm.stepGroups.forEach {
                Column {
                    Text(
                        text = "${stringResource(it.name)}: ${it.status}",
                        style = MaterialTheme.typography.titleLarge
                    )

                    it.steps.forEach {
                        Text(
                            text = "${it.name}: ${it.status}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}