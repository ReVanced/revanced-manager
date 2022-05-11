package app.revanced.manager.ui.components

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.revanced.manager.ui.components.placeholders.applist.AppIcon

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppList(applications: List<ApplicationInfo>) {
    val pm = LocalContext.current.packageManager
    LazyColumn {
        items(count = applications.size) {
            val app = applications[it]
            val label = pm.getApplicationLabel(app).toString()
            val packageName = app.packageName

            val same = packageName == label
            ListItem(icon = {
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