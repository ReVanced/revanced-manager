package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NonSuggestedVersionDialog
import app.revanced.manager.ui.component.SearchView
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.transparentListItemColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSelectorScreen(
    onSelect: (String) -> Unit,
    onStorageSelect: (SelectedApp.Local) -> Unit,
    onBackClick: () -> Unit,
    vm: AppSelectorViewModel = koinViewModel()
) {
    EventEffect(flow = vm.storageSelectionFlow) {
        onStorageSelect(it)
    }

    val pickApkLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(vm::handleStorageResult)
        }

    val suggestedVersions by vm.suggestedAppVersions.collectAsStateWithLifecycle()

    var search by rememberSaveable { mutableStateOf(false) }
    val appList by vm.apps.collectAsStateWithLifecycle()
    val appsListFiltered by vm.filteredApps.collectAsStateWithLifecycle()

    vm.nonSuggestedVersionDialogSubject?.let {
        NonSuggestedVersionDialog(
            suggestedVersion = suggestedVersions[it.packageName].orEmpty(),
            onDismiss = vm::dismissNonSuggestedVersionDialog
        )
    }

    if (search) {
        val filterText by vm.filterText.collectAsState()

        SearchView(
            query = filterText,
            onQueryChange = vm::setFilterText,
            onActiveChange = { search = it },
            placeholder = { Text(stringResource(R.string.search_apps)) }
        ) {
            val appsFiltered = appsListFiltered
            if (!appsFiltered.isNullOrEmpty() && filterText.isNotEmpty()) {
                LazyColumnWithScrollbar(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = appsFiltered,
                        key = { it.packageName }
                    ) { app ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onSelect(app.packageName)
                            },
                            leadingContent = {
                                AppIcon(
                                    packageInfo = app.packageInfo,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            headlineContent = { AppLabel(app.packageInfo) },
                            supportingContent = { Text(app.packageName) },
                            trailingContent = app.patches?.let {
                                {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.patch_count,
                                            it,
                                            it
                                        )
                                    )
                                }
                            },
                            colors = transparentListItemColors
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.type_anything),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_app),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { search = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.Search, stringResource(R.string.search))
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        pickApkLauncher.launch(APK_MIMETYPE)
                    },
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
                    }
                )
                HorizontalDivider()
            }

            val apps = appList
            if (apps == null) {
                item(key = "LOADING") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            } else if (apps.isNotEmpty()) {
                items(
                    items = apps,
                    key = { "APP-" + it.packageName },
                    contentType = { "APP" },
                ) { app ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onSelect(app.packageName)
                        },
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
                        supportingContent = {
                            suggestedVersions[app.packageName]?.let {
                                Text(stringResource(R.string.suggested_version_info, it))
                            }
                        },
                        trailingContent = app.patches?.let {
                            {
                                Text(
                                    pluralStringResource(
                                        R.plurals.patch_count,
                                        it,
                                        it
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}