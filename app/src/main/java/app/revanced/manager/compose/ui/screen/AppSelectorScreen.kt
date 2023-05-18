package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppIcon
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.component.LoadingIndicator
import app.revanced.manager.compose.util.PM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    onAppClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var filterText by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }

    // TODO: find something better for this
    if (search) {
        SearchBar(
            query = filterText,
            onQueryChange = { filterText = it },
            onSearch = {  },
            active = true,
            onActiveChange = { search = it },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { IconButton({ search = false }) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) } },
            shape = SearchBarDefaults.inputFieldShape,
            content = {
                if (PM.appList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            PM.appList
                                .filter { app ->
                                    (app.label.contains(
                                        filterText,
                                        true
                                    ) or app.packageName.contains(filterText, true))
                                }
                        ) { app ->

                            ListItem(
                                modifier = Modifier.clickable { onAppClick() },
                                leadingContent = { AppIcon(app.icon, null, 36) },
                                headlineContent = { Text(app.label) },
                                supportingContent = { Text(app.packageName) },
                                trailingContent = { Text((PM.testList[app.packageName]?: 0).let { if (it == 1) "$it " + stringResource(R.string.patch) else "$it " + stringResource(R.string.patches) }) }
                            )

                        }
                    }
                } else {
                    LoadingIndicator()
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
                    IconButton(onClick = {  }) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = { search = true }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (PM.supportedAppList.isNotEmpty()) {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {

                        ListItem(
                            modifier = Modifier.clickable { },
                            leadingContent = { Box(Modifier.size(36.dp), Alignment.Center) { Icon(Icons.Default.Storage, null, modifier = Modifier.size(24.dp)) } },
                            headlineContent = { Text(stringResource(R.string.select_from_storage)) }
                        )

                        Divider()

                    }

                    (PM.appList.ifEmpty { PM.supportedAppList }).also { list ->
                        items(
                            count = list.size,
                            key = { list[it].packageName }
                        ) { index ->

                            val app = list[index]

                            ListItem(
                                modifier = Modifier.clickable { onAppClick() },
                                leadingContent = { AppIcon(app.icon, null, 36) },
                                headlineContent = { Text(app.label) },
                                supportingContent = { Text(app.packageName) },
                                trailingContent = {
                                    Text(
                                        (PM.testList[app.packageName]?: 0).let { if (it == 1) "$it " + stringResource(R.string.patch) else "$it " + stringResource(R.string.patches) }
                                    )
                                }
                            )

                        }

                        if (PM.appList.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                    CircularProgressIndicator(Modifier.padding(vertical = 15.dp).size(24.dp), strokeWidth = 3.dp)
                                }
                            }
                        }
                    }
                }

            } else {
                LoadingIndicator()
            }
        }
    }
}



                /*Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("Patched apps") },
                        leadingIcon = { Icon(Icons.Default.Check, null) },
                        enabled = false
                    )
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("User apps") },
                        leadingIcon = { Icon(Icons.Default.Android, null) }
                    )
                    FilterChip(
                        selected = filterSystemApps,
                        onClick = { filterSystemApps = !filterSystemApps },
                        label = { Text("System apps") },
                        leadingIcon = { Icon(Icons.Default.Apps, null) }
                    )
                }*/
