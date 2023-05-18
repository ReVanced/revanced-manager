package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.component.GroupHeader
import app.revanced.manager.compose.ui.viewmodel.PatchesSelectorViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PatchesSelectorScreen(
    selectedApp: List<File>? = null,
    onBackClick: () -> Unit,
    viewModel: PatchesSelectorViewModel = getViewModel()
) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    var showOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsupportedDialog by rememberSaveable { mutableStateOf(false) }

    if (showUnsupportedDialog)
        UnsupportedDialog(onDismissRequest = { showUnsupportedDialog = false })

    if (showOptionsDialog)
        OptionsDialog(
            onDismissRequest = { showOptionsDialog = false },
            onConfirm = {}
        )

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_patches),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = {  }) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = {  }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = { Icon(Icons.Default.Build, null) },
                onClick = { /*TODO*/ })
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                viewModel.bundles.forEachIndexed { index, bundle ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(bundle.name) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                pageCount = viewModel.bundles.size,
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->

                    val patches = rememberSaveable { viewModel.bundles[index].patches }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        items(
                            items = patches["supported"]!!
                        ) { patch ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    if (viewModel.selectedPatches.contains(patch))
                                        viewModel.selectedPatches.remove(patch)
                                    else
                                        viewModel.selectedPatches.add(patch)
                                },
                                leadingContent = {
                                    Checkbox(
                                        checked = viewModel.selectedPatches.contains(patch),
                                        onCheckedChange = {
                                            if (viewModel.selectedPatches.contains(patch))
                                                viewModel.selectedPatches.remove(patch)
                                            else
                                                viewModel.selectedPatches.add(patch)
                                        }
                                    )
                                },
                                headlineContent = {
                                    Text(patch.name)
                                },
                                supportingContent = {
                                    Text(patch.description)
                                },
                                trailingContent = {
                                    if (patch.options.isNotEmpty()) {
                                        IconButton(onClick = { showOptionsDialog = true }) {
                                            Icon(Icons.Outlined.Settings, null)
                                        }
                                    }
                                }
                            )
                        }

                        if (patches["unsupported"]!!.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .padding(end = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    GroupHeader("Unsupported patches", Modifier.padding(0.dp))
                                    IconButton(onClick = { showUnsupportedDialog = true }) {
                                        Icon(
                                            Icons.Outlined.HelpOutline,
                                            stringResource(R.string.help)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = patches["unsupported"]!!,
                            // key = { it.name }
                        ) { patch ->

                            ListItem(
                                modifier = Modifier
                                    .alpha(0.5f)
                                    .clickable(enabled = false) {
                                        if (viewModel.selectedPatches.contains(patch))
                                            viewModel.selectedPatches.remove(patch)
                                        else
                                            viewModel.selectedPatches.add(patch)
                                    },
                                leadingContent = {
                                    Checkbox(
                                        checked = viewModel.selectedPatches.contains(patch),
                                        onCheckedChange = {
                                            if (viewModel.selectedPatches.contains(patch))
                                                viewModel.selectedPatches.remove(patch)
                                            else
                                                viewModel.selectedPatches.add(patch)
                                        },
                                        enabled = false
                                    )
                                },
                                headlineContent = {
                                    Text(patch.name)
                                },
                                supportingContent = {
                                    Text(patch.description)
                                },
                                trailingContent = {
                                    if (patch.options.isNotEmpty()) {
                                        IconButton(onClick = { showOptionsDialog = true }, enabled = false) {
                                            Icon(Icons.Outlined.Settings, null)
                                        }
                                    }
                                }
                            )
                        }
                    }




                }
            )
        }
    }
}

@Composable
fun UnsupportedDialog(
    onDismissRequest: () -> Unit
) {
    val appVersion = "1.1.0"
    val supportedVersions = listOf("1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0")

    AlertDialog(
        modifier = Modifier.padding(vertical = 45.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.ok))
            }
        },
        title = { Text(stringResource(R.string.unsupported_app)) },
        text = { Text(stringResource(R.string.app_not_supported, appVersion, supportedVersions.joinToString(", "))) }
    )
}

@Composable
fun OptionsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismissRequest()
            }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        title = { Text(stringResource(R.string.options)) },
        text = {
            Text("You really thought these would exist?")
        }
    )
}