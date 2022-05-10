package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.ui.components.AppBar
import app.revanced.manager.ui.components.AppList
import app.revanced.manager.ui.components.DialogAppBar
import app.revanced.manager.ui.components.Navigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen() {
    Scaffold(
        topBar = {
            DialogAppBar("Select an app")
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                AppList()
            }

        })
}

@Preview
@Composable
fun PreviewAppSelectorScreen() {
    AppSelectorScreen()
}