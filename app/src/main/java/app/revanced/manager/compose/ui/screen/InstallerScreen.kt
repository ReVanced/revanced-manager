package app.revanced.manager.compose.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppScaffold
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.viewmodel.InstallerScreenViewModel
import app.revanced.manager.compose.util.APK_MIMETYPE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    onBackClick: () -> Unit,
    vm: InstallerScreenViewModel
) {
    val exportApkLauncher = rememberLauncherForActivityResult(CreateDocument(APK_MIMETYPE), vm::export)

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.installer),
                onBackClick = onBackClick,
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

            Button(
                onClick = vm::installApk,
                enabled = vm.canInstall
            ) {
                Text(stringResource(R.string.install_app))
            }

            Button(
                onClick = { exportApkLauncher.launch("${vm.packageName}.apk") },
                enabled = vm.canInstall
            ) {
                Text(stringResource(R.string.export_app))
            }
        }
    }
}