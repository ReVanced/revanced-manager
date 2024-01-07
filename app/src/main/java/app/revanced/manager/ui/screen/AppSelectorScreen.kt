package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.toast
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    onAppClick: (packageName: String) -> Unit,
    onStorageClick: (SelectedApp.Local) -> Unit,
    onBackClick: () -> Unit,
    vm: AppSelectorViewModel = getViewModel()
) {
    val context = LocalContext.current

    val pickApkLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { apkUri ->
                vm.loadSelectedFile(apkUri)?.let(onStorageClick) ?: context.toast(
                    context.getString(
                        R.string.failed_to_load_apk
                    )
                )
            }
        }

    var filterText by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }

    val appList by vm.appList.collectAsStateWithLifecycle(initialValue = emptyList())
    val filteredAppList = remember(appList, filterText) {
        appList.filter { app ->
            (vm.loadLabel(app.packageInfo)).contains(
                filterText,
                true
            ) or app.packageName.contains(filterText, true)
        }
    }

    // TODO: find something better for this
    if (search) {
        SearchBar(
            query = filterText,
            onQueryChange = { filterText = it },
            onSearch = { },
            active = true,
            onActiveChange = { search = it },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = {
                IconButton({ search = false }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back)
                    )
                }
            },
            content = {
                if (appList.isNotEmpty() && filterText.isNotEmpty()) {
                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredAppList,
                            key = { it.packageName }
                        ) { app ->
                            ListItem(
                                modifier = Modifier.clickable { onAppClick(app.packageName) },
                                leadingContent = {
                                    AppIcon(
                                        app.packageInfo,
                                        null,
                                        Modifier.size(36.dp)
                                    )
                                },
                                headlineContent = { AppLabel(app.packageInfo) },
                                supportingContent = { Text(app.packageName) },
                                trailingContent = app.patches?.let {
                                    {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.patches_count,
                                                it,
                                                it
                                            )
                                        )
                                    }
                                }
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
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search),
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = stringResource(R.string.type_anything),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_app),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { search = true }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        }
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

            if (appList.isNotEmpty()) {
                items(
                    items = appList,
                    key = { it.packageName }
                ) { app ->
                    ListItem(
                        modifier = Modifier.clickable { onAppClick(app.packageName) },
                        leadingContent = { AppIcon(app.packageInfo, null, Modifier.size(36.dp)) },
                        headlineContent = { AppLabel(app.packageInfo) },
                        supportingContent = { Text(app.packageName) },
                        trailingContent = app.patches?.let {
                            {
                                Text(
                                    pluralStringResource(
                                        R.plurals.patches_count,
                                        it,
                                        it
                                    )
                                )
                            }
                        }
                    )

                }
            } else {
                item { LoadingIndicator() }
            }
        }
    }
}