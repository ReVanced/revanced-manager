package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R
import app.revanced.manager.ui.components.AppList
import app.revanced.manager.ui.components.DialogAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen() {
    val applications = LocalContext.current.packageManager.getInstalledApplications(0)

    Scaffold(
        topBar = {
            DialogAppBar(stringResource(id = R.string.app_selector_title))
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                AppList(applications)
            }
        }
    )
}

@Preview
@Composable
fun PreviewAppSelectorScreen() {
    AppSelectorScreen()
}