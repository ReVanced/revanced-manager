package app.revanced.manager.ui.screen

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import app.revanced.manager.ui.viewmodel.AppsViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import com.eygraber.compose.placeholder.placeholder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    onAppClick: (InstalledApp) -> Unit,
    onPatchableAppClick: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
    viewModel: AppsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    EventEffect(flow = viewModel.storageSelectionFlow) {
        onStorageSelect(it)
    }

    val pickApkLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(viewModel::handleStorageResult)
        }

    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val patchableApps by viewModel.patchableApps.collectAsStateWithLifecycle()
    val suggestedVersions by viewModel.suggestedVersions.collectAsStateWithLifecycle()
    val pinnedApps by viewModel.pinnedApps.collectAsStateWithLifecycle()

    val patchedPackageNames by remember {
        derivedStateOf {
            installedApps
                ?.flatMap { listOf(it.currentPackageName, it.originalPackageName) }
                ?.toSet()
                .orEmpty()
        }
    }

    fun InstalledApp.matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true

        val packageInfo = viewModel.packageInfoMap[currentPackageName]
        return currentPackageName.contains(query, ignoreCase = true) ||
            originalPackageName.contains(query, ignoreCase = true) ||
            viewModel.loadLabel(packageInfo).contains(query, ignoreCase = true)
    }

    fun patchableMatchesQuery(packageName: String, label: String?, query: String): Boolean {
        if (query.isBlank()) return true

        return packageName.contains(query, ignoreCase = true) ||
            label?.contains(query, ignoreCase = true) == true
    }

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val filterText by viewModel.filterText.collectAsStateWithLifecycle()

    val TITLE_HORIZONTAL = 16.dp
    val TITLE_VERTICAL = 8.dp

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(horizontal = if (searchExpanded) 0.dp else 16.dp)) {
                SearchBar(
                    query = filterText,
                    onQueryChange = viewModel::setFilterText,
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    leadingIcon = {
                        TooltipIconButton(
                            onClick = {
                                if (searchExpanded) {
                                    searchExpanded = false
                                    viewModel.setFilterText("")
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
                                onClick = { viewModel.setFilterText("") },
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
                    val filteredPatchedApps = patched
                        ?.filter { it.matchesQuery(query) }
                        .orEmpty()
                    val filteredPatchableApps = patchable
                        ?.filter { app ->
                            app.packageName !in patchedPackageNames &&
                                patchableMatchesQuery(
                                    packageName = app.packageName,
                                    label = viewModel.loadLabel(app.packageInfo),
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
                                val pinnedFilteredPatched = filteredPatchedApps.filter { it.currentPackageName in pinnedApps }
                                val pinnedFilteredPatchable = filteredPatchableApps.filter { it.packageName in pinnedApps }
                                val hasPinnedResults = pinnedFilteredPatched.isNotEmpty() || pinnedFilteredPatchable.isNotEmpty()

                                val unpinnedFilteredPatched = filteredPatchedApps.filter { it.currentPackageName !in pinnedApps }
                                val unpinnedFilteredPatchable = filteredPatchableApps.filter { it.packageName !in pinnedApps }

                                if (hasPinnedResults) {
                                    item(key = "SEARCH_HEADER_PINNED") {
                                        Row(
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp).rotate(45f),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = stringResource(R.string.pinned_apps_section_title),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }

                                    items(
                                        items = pinnedFilteredPatched,
                                        key = { "SEARCH_PATCHED-${it.currentPackageName}" },
                                        contentType = { "SEARCH_PATCHED" }
                                    ) { installedApp ->
                                        val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                                        ListItem(
                                            modifier = Modifier
                                                .animateItem()
                                                .combinedClickable(
                                                    onClick = {
                                                        searchExpanded = false
                                                        viewModel.setFilterText("")
                                                        onAppClick(installedApp)
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePinned(installedApp.currentPackageName)
                                                    }
                                                ),
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

                                    items(
                                        items = pinnedFilteredPatchable,
                                        key = { "SEARCH_PATCHABLE-${it.packageName}" },
                                        contentType = { "SEARCH_PATCHABLE" }
                                    ) { app ->
                                        ListItem(
                                            modifier = Modifier
                                                .animateItem()
                                                .combinedClickable(
                                                    onClick = {
                                                        searchExpanded = false
                                                        viewModel.setFilterText("")
                                                        onPatchableAppClick(app.packageName)
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePinned(app.packageName)
                                                    }
                                                ),
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
                                                    val version = if (app.packageName in suggestedVersions)
                                                        suggestedVersions[app.packageName] ?: stringResource(R.string.any_version)
                                                    else null
                                                    SupportingPills(
                                                        patchCount = patchCount,
                                                        suggestedVersion = version
                                                    )
                                                }
                                            },
                                            trailingContent = if (app.packageInfo == null) {
                                                { Text(stringResource(R.string.not_installed)) }
                                            } else null,
                                            colors = transparentListItemColors
                                        )
                                    }

                                    if (unpinnedFilteredPatched.isNotEmpty() || unpinnedFilteredPatchable.isNotEmpty()) {
                                        item(key = "SEARCH_HEADER_AVAILABLE") {
                                            Row(
                                                modifier = Modifier
                                                    .animateItem()
                                                    .fillMaxWidth()
                                                    .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Apps,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = stringResource(R.string.available_apps_section_title),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = unpinnedFilteredPatched,
                                    key = { "SEARCH_PATCHED-${it.currentPackageName}" },
                                    contentType = { "SEARCH_PATCHED" }
                                ) { installedApp ->
                                    val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                                    ListItem(
                                        modifier = Modifier
                                            .animateItem()
                                            .combinedClickable(
                                                onClick = {
                                                    searchExpanded = false
                                                    viewModel.setFilterText("")
                                                    onAppClick(installedApp)
                                                },
                                                onLongClick = {
                                                    viewModel.togglePinned(installedApp.currentPackageName)
                                                }
                                            ),
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

                                items(
                                    items = unpinnedFilteredPatchable,
                                    key = { "SEARCH_PATCHABLE-${it.packageName}" },
                                    contentType = { "SEARCH_PATCHABLE" }
                                ) { app ->
                                    ListItem(
                                        modifier = Modifier
                                            .animateItem()
                                            .combinedClickable(
                                                onClick = {
                                                    searchExpanded = false
                                                    viewModel.setFilterText("")
                                                    onPatchableAppClick(app.packageName)
                                                },
                                                onLongClick = {
                                                    viewModel.togglePinned(app.packageName)
                                                }
                                            ),
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
                                                val version = if (app.packageName in suggestedVersions)
                                                    suggestedVersions[app.packageName] ?: stringResource(R.string.any_version)
                                                else null
                                                SupportingPills(
                                                    patchCount = patchCount,
                                                    suggestedVersion = version
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

            val allPatchableApps = patchable.filter { it.packageName !in patchedPackageNames }

            val pinnedPatchedApps = patched.filter { it.currentPackageName in pinnedApps }
            val pinnedPatchableApps = allPatchableApps.filter { it.packageName in pinnedApps }
            val hasPinnedApps = pinnedPatchedApps.isNotEmpty() || pinnedPatchableApps.isNotEmpty()

            val unpinnedPatchedApps = patched.filter { it.currentPackageName !in pinnedApps }
            val unpinnedPatchableApps = allPatchableApps.filter { it.packageName !in pinnedApps }

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

            if (hasPinnedApps) {
                item(key = "HEADER_PINNED") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).rotate(45f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.pinned_apps_section_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                items(
                    items = pinnedPatchedApps,
                    key = { "PATCHED-${it.currentPackageName}" },
                    contentType = { "PATCHED" },
                ) { installedApp ->
                    val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                    ListItem(
                        modifier = Modifier
                            .animateItem()
                            .combinedClickable(
                                onClick = { onAppClick(installedApp) },
                                onLongClick = { viewModel.togglePinned(installedApp.currentPackageName) }
                            ),
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

                items(
                    items = pinnedPatchableApps,
                    key = { "PATCHABLE-${it.packageName}" },
                    contentType = { "PATCHABLE" },
                ) { app ->
                    ListItem(
                        modifier = Modifier
                            .animateItem()
                            .combinedClickable(
                                onClick = { onPatchableAppClick(app.packageName) },
                                onLongClick = { viewModel.togglePinned(app.packageName) }
                            ),
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
                                val version = if (app.packageName in suggestedVersions)
                                    suggestedVersions[app.packageName] ?: stringResource(R.string.any_version)
                                else null
                                SupportingPills(
                                    patchCount = patchCount,
                                    suggestedVersion = version
                                )
                            }
                        },
                        trailingContent = if (app.packageInfo == null) {
                            { Text(stringResource(R.string.not_installed)) }
                        } else null,
                        colors = transparentListItemColors
                    )
                }

                item(key = "HEADER_AVAILABLE") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = TITLE_HORIZONTAL, vertical = TITLE_VERTICAL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.available_apps_section_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            items(
                items = unpinnedPatchedApps,
                key = { "PATCHED-${it.currentPackageName}" },
                contentType = { "PATCHED" },
            ) { installedApp ->
                val packageInfo = viewModel.packageInfoMap[installedApp.currentPackageName]

                ListItem(
                    modifier = Modifier
                        .animateItem()
                        .combinedClickable(
                            onClick = { onAppClick(installedApp) },
                            onLongClick = { viewModel.togglePinned(installedApp.currentPackageName) }
                        ),
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

            items(
                items = unpinnedPatchableApps,
                key = { "PATCHABLE-${it.packageName}" },
                contentType = { "PATCHABLE" },
            ) { app ->
                ListItem(
                    modifier = Modifier
                        .animateItem()
                        .combinedClickable(
                            onClick = { onPatchableAppClick(app.packageName) },
                            onLongClick = { viewModel.togglePinned(app.packageName) }
                        ),
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
                            val version = if (app.packageName in suggestedVersions)
                                suggestedVersions[app.packageName] ?: stringResource(R.string.any_version)
                            else null
                            SupportingPills(
                                patchCount = patchCount,
                                suggestedVersion = version
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

@Composable
fun SupportingPills(patchCount: Int, suggestedVersion: String? = null) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (patchCount > 0) {
            PatchesPill(patchCount = patchCount, isPatched = false)
        }
        if (suggestedVersion != null) {
            if (patchCount > 0) Spacer(Modifier.width(4.dp))
            VersionPill(version = suggestedVersion, isPatched = false)
        }
    }
}

@Composable
private fun PatchesPill(patchCount: Int, isPatched: Boolean = false) {
    Pill(pluralStringResource(R.plurals.patch_count, patchCount, patchCount), isPatched)
}

@Composable
private fun VersionPill(version: String, isPatched: Boolean) {
    Pill(version, isPatched)
}

@Composable
private fun Pill(text: String?, isPatched: Boolean) {
    SuggestionChip(
        onClick = { /* nothing... */ },
        label = {
            Text(
                text = text ?: stringResource(R.string.loading),
                modifier = Modifier.placeholder(
                    visible = text == null,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = MaterialTheme.shapes.extraSmall
                ),
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier.height(24.dp),
        enabled = false,
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledContainerColor = if (isPatched) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            disabledLabelColor = if (isPatched) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = null,
    )
}
