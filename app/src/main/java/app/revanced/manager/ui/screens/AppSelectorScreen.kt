package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.components.AppList
import app.revanced.manager.ui.components.DialogAppBar
import app.revanced.manager.ui.components.placeholders.applist.AppIcon
import app.revanced.manager.ui.screens.destinations.DashboardSubscreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.utils.startDestination

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
@Destination
@RootNavGraph
@Composable
fun AppSelectorScreen(
    applications: Array<ApplicationInfo>,
    resultNavigator: ResultBackNavigator<String>
) {
    val pm = LocalContext.current.packageManager
    val rl = rememberLazyListState()
    rl.interactionSource
    LazyColumn {
        items(count = applications.size) {
            val app = applications[it]
            val label = pm.getApplicationLabel(app).toString()
            val packageName = app.packageName

            val same = packageName == label
            ListItem(modifier = Modifier.clickable {
                resultNavigator.navigateBack(result = applications[it].packageName)
            },
                icon = {
                    AppIcon(
                        app.loadIcon(pm),
                        packageName
                    )
                }, text = {
                    if (same) {
                        Text(packageName)
                    } else {
                        Text(label)
                    }
                }, secondaryText = {
                    if (!same) {
                        Text(packageName)
                    }
                })
        }
    }
}