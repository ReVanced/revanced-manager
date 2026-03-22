package app.revanced.manager.ui.screen

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.ui.viewmodel.InstalledAppsViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstalledAppsScreen(
    onAppClick: (InstalledApp) -> Unit,
    onPatchableAppClick: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
    viewModel: InstalledAppsViewModel = koinViewModel(),
    selectorVm: AppSelectorViewModel = koinViewModel()
) {
    val context = LocalContext.current
    EventEffect(flow = selectorVm.storageSelectionFlow) {
        onStorageSelect(it)
    }

    val pickApkLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(selectorVm::handleStorageResult)
        }

    val installedApps by viewModel.apps.collectAsStateWithLifecycle()
    val patchableApps by selectorVm.apps.collectAsStateWithLifecycle()

    fun patchedPackageNames(apps: List<InstalledApp>?): Set<String> =
        apps
            ?.flatMap { listOf(it.currentPackageName, it.originalPackageName) }
            ?.toSet()
            .orEmpty()

    fun InstalledApp.matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true

        val packageInfo = viewModel.packageInfoMap[currentPackageName]
        return currentPackageName.contains(query, ignoreCase = true) ||
            originalPackageName.contains(query, ignoreCase = true) ||
            selectorVm.loadLabel(packageInfo).contains(query, ignoreCase = true)
    }

    fun patchableMatchesQuery(packageName: String, label: String?, query: String): Boolean {
        if (query.isBlank()) return true

        return packageName.contains(query, ignoreCase = true) ||
            label?.contains(query, ignoreCase = true) == true
    }

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val filterText by selectorVm.filterText.collectAsStateWithLifecycle()

    val TITLE_HORIZONTAL = 16.dp
    val TITLE_VERTICAL = 8.dp

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(horizontal = if (searchExpanded) 0.dp else 16.dp)) {
                SearchBar(
                    query = filterText,
                    onQueryChange = selectorVm::setFilterText,
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    leadingIcon = {
                        TooltipIconButton(
                            onClick = {
                                if (searchExpanded) {
                                    searchExpanded = false
                                    selectorVm.setFilterText("")
                                }
                            },
                            tooltip = if (searchExpanded) stringResource(R.string.back) else stringResource(R.string.search),
                        ) { _ ->
                            Crossfade(
                                targetState = searchExpanded,
                                label = "SearchIcon"
                            ) { expanded ->
                                Icon(
                                    imageVector = if (expanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.Search,
                                    contentDescription = if (expanded) stringResource(R.string.back) else stringResource(R.string.search)
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (searchExpanded && filterText.isNotEmpty()) {
                            TooltipIconButton(
                                onClick = { selectorVm.setFilterText("") },
                                tooltip = stringResource(R.string.clear),
                            ) { contentDescription ->
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = contentDescription
                                )
                            }
                        }
                    },
                ) {
                    val query = filterText.trim()
                    val patched = installedApps
                    val patchable = patchableApps
                    val patchedPkgNames = patchedPackageNames(patched)
                    val filteredPatchedApps = patched
                        ?.filter { it.matchesQuery(query) }
                        .orEmpty()
                    val filteredPatchableApps = patchable
                        ?.filter { app ->
                            app.packageName !in patchedPkgNames &&
                                patchableMatchesQuery(
                                    packageName = app.packageName,
                                    label = selectorVm.loadLabel(app.packageInfo),
                                    query = query
                                )
                        }
                        .orEmpty()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (patched == null || patchable == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        } else {
                            LazyColumnWithScrollbar(modifier = Modifier.fillMaxSize()) {
                                if (filteredPatchedApps.isNotEmpty()) {
                                    item(key = "SEARCH_HEADER_PATCHED") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stringResource(R.string.patched_apps_section_title),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = filteredPatchedApps,
                                    key = { "SEARCH_PATCHED-${it.currentPackageName}" },
                                    contentType = { "SEARCH_PATCHED" }
                                ) { installedApp ->
                                    val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                                    ListItem(
                                        modifier = Modifier.clickable {
                                            searchExpanded = false
                                            selectorVm.setFilterText("")
                                            onAppClick(installedApp)
                                        },
                                        leadingContent = {
                                            AppIcon(
                                                packageInfo = packageInfo,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        },
                                        headlineContent = {
                                            AppLabel(packageInfo, defaultText = installedApp.currentPackageName)
                                        },
                                        supportingContent = {
                                            Text(installedApp.currentPackageName)
                                        },
                                        colors = transparentListItemColors
                                    )
                                }

                                if (filteredPatchableApps.isNotEmpty()) {
                                    item(key = "SEARCH_HEADER_PATCHABLE") {
                                        Text(
                                            text = stringResource(R.string.patchable_apps_section_title),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL)
                                        )
                                    }
                                }

                                items(
                                    items = filteredPatchableApps,
                                    key = { "SEARCH_PATCHABLE-${it.packageName}" },
                                    contentType = { "SEARCH_PATCHABLE" }
                                ) { app ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            searchExpanded = false
                                            selectorVm.setFilterText("")
                                            onPatchableAppClick(app.packageName)
                                        },
                                        leadingContent = {
                                            AppIcon(
                                                packageInfo = app.packageInfo,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        },
                                        headlineContent = {
                                            AppLabel(app.packageInfo, defaultText = app.packageName)
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
                    }
                }
            }
        }
    ) { paddingValues ->
        if (searchExpanded) return@Scaffold

        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = TITLE_VERTICAL),
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

            if (patched.isNotEmpty()) {
                item(key = "HEADER_PATCHED") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.patched_apps_section_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

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
            }

            if (patched.isNotEmpty()) {
                item(key = "HEADER_PATCHABLE") {
                    Text(
                        text = stringResource(R.string.patchable_apps_section_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL)
                    )
                }
            }

            item(key = "PATCHABLE_STORAGE") {
                ListItem(
                    modifier = Modifier.clickable { try {
                        pickApkLauncher.launch(APK_MIMETYPE)
                    } catch (_: ActivityNotFoundException) {
                        context.toast(R.string.no_file_picker_found)
                    } },
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
}