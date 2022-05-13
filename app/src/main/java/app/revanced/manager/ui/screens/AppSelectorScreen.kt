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
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.components.AppList
import app.revanced.manager.ui.components.DialogAppBar
import app.revanced.manager.ui.screens.destinations.DashboardSubscreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.utils.startDestination

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AppSelectorScreen(titleText: String, filter: Array<String>, navigator: DestinationsNavigator) {
    val applications = LocalContext.current.packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    Scaffold(
        topBar = { DialogAppBar(titleText, { navigator.navigate(DashboardSubscreenDestination) }) }, // (baiorett) TODO: find a way to do it better because that's not the best way to do it and i tried resultnavigator which returns back with a boolean but in this situation you cant just make it go back for some reason it just doesnt work and ill have to find out why it does not work later alright?
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
//    AppSelectorScreen(
//        stringResource(id = R.string.app_selector_title),
//        filter = arrayOf("placeholder")
//    )
}