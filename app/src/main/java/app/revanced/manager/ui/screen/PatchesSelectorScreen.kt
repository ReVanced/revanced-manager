package app.revanced.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_SUPPORTED
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNSUPPORTED
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchesSelection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PatchesSelectorScreen(
    onPatchClick: (PatchesSelection, Options) -> Unit,
    onBackClick: () -> Unit,
    vm: PatchesSelectorViewModel
) {
    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    val bundles by vm.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    if (vm.compatibleVersions.isNotEmpty())
        UnsupportedDialog(
            appVersion = vm.selectedApp.version,
            supportedVersions = vm.compatibleVersions,
            onDismissRequest = vm::dismissDialogs
        )

    vm.optionsDialog?.let { (bundle, patch) ->
        OptionsDialog(
            onDismissRequest = vm::dismissDialogs,
            patch = patch,
            values = vm.getOptions(bundle, patch),
            reset = { vm.resetOptions(bundle, patch) },
            set = { key, value -> vm.setOption(bundle, patch, key, value) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_patches),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = { Icon(Icons.Default.Build, null) },
                onClick = {
                    composableScope.launch {
                        // TODO: only allow this if all required options have been set.
                        onPatchClick(vm.getAndSaveSelection(), vm.getOptions())
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (bundles.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
                ) {
                    bundles.forEachIndexed { index, bundle ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                composableScope.launch {
                                    pagerState.animateScrollToPage(
                                        index
                                    )
                                }
                            },
                            text = { Text(bundle.name) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalPager(
                pageCount = bundles.size,
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->
                    val bundle = bundles[index]

                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FilterChip(
                                selected = vm.filter and SHOW_SUPPORTED != 0 && bundle.supported.isNotEmpty(),
                                onClick = { vm.toggleFlag(SHOW_SUPPORTED) },
                                label = { Text(stringResource(R.string.supported)) },
                                enabled = bundle.supported.isNotEmpty()
                            )

                            FilterChip(
                                selected = vm.filter and SHOW_UNIVERSAL != 0 && bundle.universal.isNotEmpty(),
                                onClick = { vm.toggleFlag(SHOW_UNIVERSAL) },
                                label = { Text(stringResource(R.string.universal)) },
                                enabled = bundle.universal.isNotEmpty()
                            )

                            FilterChip(
                                selected = vm.filter and SHOW_UNSUPPORTED != 0 && bundle.unsupported.isNotEmpty(),
                                onClick = { vm.toggleFlag(SHOW_UNSUPPORTED) },
                                label = { Text(stringResource(R.string.unsupported)) },
                                enabled = bundle.unsupported.isNotEmpty()
                            )
                        }

                        val allowExperimental by vm.allowExperimental.getAsState()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            fun LazyListScope.patchList(
                                patches: List<PatchInfo>,
                                filterFlag: Int,
                                supported: Boolean,
                                header: (@Composable () -> Unit)? = null
                            ) {
                                if (patches.isNotEmpty() && (vm.filter and filterFlag) != 0 || vm.filter == 0) {
                                    header?.let {
                                        item {
                                            it()
                                        }
                                    }

                                    items(
                                        items = patches,
                                        key = { it.name }
                                    ) { patch ->
                                        PatchItem(
                                            patch = patch,
                                            onOptionsDialog = {
                                                vm.optionsDialog = bundle.uid to patch
                                            },
                                            selected = supported && vm.isSelected(
                                                bundle.uid,
                                                patch
                                            ),
                                            onToggle = { vm.togglePatch(bundle.uid, patch) },
                                            supported = supported
                                        )
                                    }
                                }
                            }

                            patchList(
                                patches = bundle.supported,
                                filterFlag = SHOW_SUPPORTED,
                                supported = true
                            )
                            patchList(
                                patches = bundle.universal,
                                filterFlag = SHOW_UNIVERSAL,
                                supported = true
                            ) {
                                ListHeader(
                                    title = stringResource(R.string.universal_patches),
                                    onHelpClick = { }
                                )
                            }
                            patchList(
                                patches = bundle.unsupported,
                                filterFlag = SHOW_UNSUPPORTED,
                                supported = allowExperimental
                            ) {
                                ListHeader(
                                    title = stringResource(R.string.unsupported_patches),
                                    onHelpClick = { vm.openUnsupportedDialog(bundle.unsupported) }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    supported: Boolean = true
) = ListItem(
    modifier = Modifier
        .let { if (!supported) it.alpha(0.5f) else it }
        .clickable(enabled = supported, onClick = onToggle)
        .fillMaxSize(),
    leadingContent = {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = supported
        )
    },
    headlineContent = { Text(patch.name) },
    supportingContent = patch.description?.let { { Text(it) } },
    trailingContent = {
        if (patch.options?.isNotEmpty() == true) {
            IconButton(onClick = onOptionsDialog, enabled = supported) {
                Icon(Icons.Outlined.Settings, null)
            }
        }
    }
)

@Composable
fun ListHeader(
    title: String,
    onHelpClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        },
        trailingContent = onHelpClick?.let {
            {
                IconButton(onClick = onHelpClick) {
                    Icon(
                        Icons.Outlined.HelpOutline,
                        stringResource(R.string.help)
                    )
                }
            }
        }
    )
}

@Composable
fun UnsupportedDialog(
    appVersion: String,
    supportedVersions: List<String>,
    onDismissRequest: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(stringResource(R.string.unsupported_app)) },
    text = {
        Text(
            stringResource(
                R.string.app_not_supported,
                appVersion,
                supportedVersions.joinToString(", ")
            )
        )
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
    patch: PatchInfo,
    values: Map<String, Any?>?,
    reset: () -> Unit,
    set: (String, Any?) -> Unit,
    onDismissRequest: () -> Unit,
) = Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true
    )
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = patch.name,
                onBackClick = onDismissRequest,
                actions = {
                    IconButton(onClick = reset) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (patch.options == null) return@LazyColumn

            items(patch.options, key = { it.key }) { option ->
                val key = option.key
                val value =
                    if (values == null || !values.contains(key)) option.defaultValue else values[key]

                OptionItem(option = option, value = value, setValue = { set(key, it) })
            }
        }
    }
}