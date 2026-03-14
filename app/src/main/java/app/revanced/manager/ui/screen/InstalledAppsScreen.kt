package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.SearchView
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.ui.viewmodel.InstalledAppsViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.transparentListItemColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun InstalledAppsScreen(
    onAppClick: (InstalledApp) -> Unit,
    onPatchableAppClick: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
    viewModel: InstalledAppsViewModel = koinViewModel(),
    selectorVm: AppSelectorViewModel = koinViewModel()
) {
    EventEffect(flow = selectorVm.storageSelectionFlow) {
        onStorageSelect(it)
    }

    val pickApkLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(selectorVm::handleStorageResult)
        }

    val installedApps by viewModel.apps.collectAsStateWithLifecycle()
    val patchableApps by selectorVm.apps.collectAsStateWithLifecycle()
    val filteredApps by selectorVm.filteredApps.collectAsStateWithLifecycle()

    fun patchedPackageNames(apps: List<InstalledApp>?): Set<String> =
        apps
            ?.flatMap { listOf(it.currentPackageName, it.originalPackageName) }
            ?.toSet()
            .orEmpty()

    var search by rememberSaveable { mutableStateOf(false) }

    if (search) {
        val filterText by selectorVm.filterText.collectAsStateWithLifecycle()
        val patchedPackageNames = patchedPackageNames(installedApps)
        val appsFiltered = filteredApps?.filter { it.packageName !in patchedPackageNames }

        SearchView(
            query = filterText,
            onQueryChange = selectorVm::setFilterText,
            onActiveChange = { search = it },
            placeholder = { Text(stringResource(R.string.search_apps)) }
        ) {
            if (!appsFiltered.isNullOrEmpty() && filterText.isNotEmpty()) {
                LazyColumnWithScrollbar(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = appsFiltered,
                        key = { it.packageName }
                    ) { app ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onPatchableAppClick(app.packageName)
                            },
                            leadingContent = {
                                AppIcon(
                                    packageInfo = app.packageInfo,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            headlineContent = { AppLabel(app.packageInfo, defaultText = app.packageName) },
                            supportingContent = app.patches?.let { patchCount ->
                                {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.patch_count,
                                            patchCount,
                                            patchCount
                                        )
                                    )
                                }
                            },
                            trailingContent = if (app.packageInfo == null) {
                                { Text(stringResource(R.string.not_installed)) }
                            } else null,
                            colors = transparentListItemColors
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.type_anything),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    LazyColumnWithScrollbar(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val patched = installedApps
        val patchable = patchableApps

        if (patched == null || patchable == null) {
            item(key = "LOADING") {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            return@LazyColumnWithScrollbar
        }

        val patchedPackageNames = patchedPackageNames(patched)
        val visiblePatchableApps = patchable.filter { it.packageName !in patchedPackageNames }

        item(key = "HEADER_PATCHED") {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupHeader(
                    title = stringResource(R.string.patched_apps_section_title),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (patched.isNotEmpty()) {
            items(
                items = patched,
                key = { "PATCHED-${it.currentPackageName}" },
                contentType = { "PATCHED" },
            ) { installedApp ->
                val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                ListItem(
                    modifier = Modifier.clickable { onAppClick(installedApp) },
                    leadingContent = {
                        AppIcon(
                            packageInfo,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    headlineContent = { AppLabel(packageInfo, defaultText = null) },
                    supportingContent = { Text(installedApp.currentPackageName) },
                    colors = transparentListItemColors
                )
            }
        } else {
            item(key = "PATCHED_EMPTY") {
                EmptyState(
                    icon = Icons.Outlined.Apps,
                    title = R.string.no_patched_apps_found,
                    description = R.string.no_patched_apps_description
                )
            }
        }

        item(key = "HEADER_PATCHABLE") {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupHeader(
                    title = stringResource(R.string.patchable_apps_section_title),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { search = true }) {
                    Icon(Icons.Outlined.Search, stringResource(R.string.search))
                }
            }
        }

        item(key = "PATCHABLE_STORAGE") {
            ListItem(
                modifier = Modifier.clickable { pickApkLauncher.launch(APK_MIMETYPE) },
                leadingContent = {
                    Box(Modifier.size(36.dp), Alignment.Center) {
                        Icon(
                            Icons.Default.Storage,
                            null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                supportingContent = {
                    Text(stringResource(R.string.select_from_storage_description))
                },
                colors = transparentListItemColors
            )
        }

        items(
            items = visiblePatchableApps,
            key = { "PATCHABLE-${it.packageName}" },
            contentType = { "PATCHABLE" },
        ) { app ->
            ListItem(
                modifier = Modifier.clickable { onPatchableAppClick(app.packageName) },
                leadingContent = {
                    AppIcon(
                        packageInfo = app.packageInfo,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                },
                headlineContent = {
                    AppLabel(
                        app.packageInfo,
                        defaultText = app.packageName
                    )
                },
                supportingContent = app.patches?.let { patchCount ->
                    {
                        Text(
                            pluralStringResource(
                                R.plurals.patch_count,
                                patchCount,
                                patchCount
                            )
                        )
                    }
                },
                trailingContent = if (app.packageInfo == null) {
                    { Text(stringResource(R.string.not_installed)) }
                } else null,
                colors = transparentListItemColors
            )
        }
    }
}