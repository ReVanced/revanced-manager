package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AppSelectorScreen(titleText: String, filter: Array<String>) {
    val applications = LocalContext.current.packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    Scaffold(
        topBar = { DialogAppBar(titleText) },
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
    AppSelectorScreen(
        stringResource(id = R.string.app_selector_title),
        filter = arrayOf("placeholder")
    )
}