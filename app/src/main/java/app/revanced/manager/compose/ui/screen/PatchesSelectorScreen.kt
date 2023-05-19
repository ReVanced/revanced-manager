package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.compose.R
import app.revanced.manager.compose.patcher.patch.PatchInfo
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.component.GroupHeader
import app.revanced.manager.compose.ui.viewmodel.PatchesSelectorViewModel
import kotlinx.coroutines.launch

const val allowUnsupported = false

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PatchesSelectorScreen(
    startPatching: (List<String>) -> Unit, onBackClick: () -> Unit, vm: PatchesSelectorViewModel
) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    val bundles by vm.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    if (vm.showUnsupportedDialog) UnsupportedDialog(onDismissRequest = vm::dismissDialogs)

    if (vm.showOptionsDialog) OptionsDialog(onDismissRequest = vm::dismissDialogs, onConfirm = {})

    Scaffold(topBar = {
        AppTopBar(title = stringResource(R.string.select_patches), onBackClick = onBackClick, actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
            }
            IconButton(onClick = { }) {
                Icon(Icons.Outlined.Search, stringResource(R.string.search))
            }
        })
    }, floatingActionButton = {
        ExtendedFloatingActionButton(text = { Text(stringResource(R.string.patch)) },
            icon = { Icon(Icons.Default.Build, null) },
            onClick = { startPatching(vm.selectedPatches) })
    }) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                bundles.forEachIndexed { index, bundle ->
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
                pageCount = bundles.size,
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->

                    val bundle = bundles[index]

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = bundle.supported
                        ) { patch ->
                            PatchItem(
                                patch = patch,
                                onOptionsDialog = vm::openOptionsDialog,
                                onToggle = {
                                    vm.togglePatch(patch)
                                },
                                selected = vm.isSelected(patch),
                                supported = true
                            )
                        }

                        if (bundle.unsupported.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(end = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    GroupHeader(stringResource(R.string.unsupported_patches), Modifier.padding(0.dp))
                                    IconButton(onClick = vm::openUnsupportedDialog) {
                                        Icon(
                                            Icons.Outlined.HelpOutline, stringResource(R.string.help)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = bundle.unsupported,
                            // key = { it.name }
                        ) { patch ->
                            PatchItem(
                                patch = patch,
                                onOptionsDialog = vm::openOptionsDialog,
                                onToggle = {
                                    vm.togglePatch(patch)
                                },
                                selected = vm.isSelected(patch),
                                supported = allowUnsupported
                            )
                        }
                    }


                })
        }
    }
}

@Composable
fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    supported: Boolean
) {
    ListItem(
        modifier = Modifier
            .let { if (!supported) it.alpha(0.5f) else it }
            .clickable(enabled = supported, onClick = onToggle),
        leadingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = {
                    onToggle()
                },
                enabled = supported
            )
        },
        headlineContent = {
            Text(patch.name)
        },
        supportingContent = {
            Text(patch.description ?: "")
        },
        trailingContent = {
            if (patch.options?.isNotEmpty() == true) {
                IconButton(onClick = onOptionsDialog, enabled = supported) {
                    Icon(Icons.Outlined.Settings, null)
                }
            }
        }
    )
}

@Composable
fun UnsupportedDialog(
    onDismissRequest: () -> Unit
) {
    val appVersion = "1.1.0"
    val supportedVersions =
        listOf("1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0", "1.1.1", "1.2.0")

    AlertDialog(modifier = Modifier.padding(vertical = 45.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.ok))
            }
        },
        title = { Text(stringResource(R.string.unsupported_app)) },
        text = { Text(stringResource(R.string.app_not_supported, appVersion, supportedVersions.joinToString(", "))) })
}

@Composable
fun OptionsDialog(
    onDismissRequest: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = {
        Button(onClick = {
            onConfirm()
            onDismissRequest()
        }) {
            Text(stringResource(R.string.apply))
        }
    }, title = { Text(stringResource(R.string.options)) }, text = {
        Text("You really thought these would exist?")
    })
}