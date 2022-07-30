package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.placeholders.applist.AppIcon
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.patch.Patch
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

private const val tag = "AppSelector"

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded", "UnrememberedMutableState")
@Destination
@RootNavGraph
@Composable
fun AppSelectorScreen(
    navigator: NavController,
    vm: AppSelectorViewModel = viewModel(),
    pvm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val installedApps by vm.installedApps
    var query by mutableStateOf("")
    val patches by pvm.patches

    LaunchedEffect(Unit) {
        if (patches is Resource.Success) {
            val filter =
                (patches as Resource.Success<List<Class<out Patch<Data>>>>).data.flatMap { patch ->
                    (patch.compatiblePackages?.toList() ?: emptyList()).map { it.name }
                }
            vm.filterInstalledApps(filter)
            // TODO: someone capable, instead of filtering them completely out make the ones
            // that are not in the filter (don't have a patch belonging to them)
            // be grayed out and not clickable. also put the filtered apps on top of the list.
        }
    }

    when (val installedApps = installedApps) {
        is Resource.Success -> {
            val apps = installedApps.data
            LazyColumn {
                item {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        value = query,
                        onValueChange = { newValue ->
                            query = newValue
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, "Search")
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                    )
                }

                // show all apps if there is no query
                if (query.isEmpty() || query.isBlank()) {
                    items(count = apps.size) {
                        val app = apps[it]
                        val label = vm.applicationLabel(app)
                        val packageName = app.packageName

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
                // filter apps to match query
                else {
                    items(count = apps.size) {
                        val app = apps[it]
                        val label = vm.applicationLabel(app)
                        val packageName = app.packageName

                        val same = packageName == label
                        if (label.contains(query, true)) {
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

    fun filterInstalledApps(toPkgs: Iterable<String>) {
        if (installedApps.value !is Resource.Success) return
        val apps = (installedApps.value as Resource.Success<List<ApplicationInfo>>).data
        val filtered = apps.filter { app -> toPkgs.any { app.packageName == it } }
        installedApps.value = Resource.success(filtered)
    }
}