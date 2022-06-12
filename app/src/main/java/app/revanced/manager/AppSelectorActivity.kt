package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.revanced.manager.ui.components.placeholders.applist.AppIcon
import app.revanced.manager.ui.theme.ReVancedManagerTheme

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
class AppSelectorActivity(
    val applications: Array<ApplicationInfo>,
    val filter: Array<String>
) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReVancedManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
                                //resultNavigator.navigateBack(result = applications[it].packageName)
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
            }
        }
    }
}