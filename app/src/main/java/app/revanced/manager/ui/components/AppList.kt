package app.revanced.manager.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.revanced.manager.ui.components.placeholders.applist.AppIcon

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppList() {
    val applications = LocalContext.current.packageManager.getInstalledApplications(0)
    LazyColumn() {
        items(count = applications.size) {
            ListItem(icon = { AppIcon(applications[it].loadIcon(LocalContext.current.packageManager), applications[it].packageName) }, text = { Text(applications[it].packageName) })
        }
    }
}