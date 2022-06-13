package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.placeholders.applist.AppIcon
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator

private const val tag = "AppSelector"

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
@Destination
@RootNavGraph
@Composable
fun AppSelectorScreen(
    navigator: NavController, // TODO: add back button
    vm: AppSelectorViewModel = viewModel(),
    pvm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val installedApps by vm.installedApps

    when (val installedApps = installedApps) {
        is Resource.Success -> {
            val apps = installedApps.data
            LazyColumn {
                items(count = apps.size) {
                    val app = apps[it]
                    val label = vm.applicationLabel(app)
                    val packageName = app.packageName

                    if (!packageName.contains("youtube")) return@items

                    val same = packageName == label
                    ListItem(modifier = Modifier.clickable {
                        pvm.setSelectedAppPackage(app.packageName)
                        navigator.navigateUp()
                    }, icon = {
                        AppIcon(vm.loadIcon(app), packageName)
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
        else -> {}
    }
}

class AppSelectorViewModel(val app: Application) : AndroidViewModel(app) {
    val installedApps = mutableStateOf<Resource<List<ApplicationInfo>>>(Resource.Loading)

    init {
        fetchInstalledApps()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun fetchInstalledApps() {
        Log.d(tag, "Fetching applications")
        try {
            installedApps.value =
                Resource.success(app.packageManager.getInstalledApplications(PackageManager.GET_META_DATA))
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while fetching apps", e)
        }
    }

    fun applicationLabel(info: ApplicationInfo): String {
        return app.packageManager.getApplicationLabel(info).toString()
    }

    fun loadIcon(info: ApplicationInfo): Drawable? {
        return info.loadIcon(app.packageManager)
    }
}