package app.revanced.manager.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.Countdown
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_SUPPORTED
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNSUPPORTED
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import kotlinx.coroutines.launch
import org.koin.compose.rememberKoinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PatchesSelectorScreen(
    onSave: (PatchSelection?, Options) -> Unit,
    onBackClick: () -> Unit,
    vm: PatchesSelectorViewModel
) {
    val bundles by vm.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        bundles.size
    }
    val composableScope = rememberCoroutineScope()
    var search: String? by rememberSaveable {
        mutableStateOf(null)
    }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val showPatchButton by remember {
        derivedStateOf { vm.selectionIsValid(bundles) }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.patch_selector_sheet_filter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.patch_selector_sheet_filter_compat_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FilterChip(
                        selected = vm.filter and SHOW_SUPPORTED != 0,
                        onClick = { vm.toggleFlag(SHOW_SUPPORTED) },
                        label = { Text(stringResource(R.string.supported)) }
                    )

                    FilterChip(
                        selected = vm.filter and SHOW_UNIVERSAL != 0,
                        onClick = { vm.toggleFlag(SHOW_UNIVERSAL) },
                        label = { Text(stringResource(R.string.universal)) },
                    )

                    FilterChip(
                        selected = vm.filter and SHOW_UNSUPPORTED != 0,
                        onClick = { vm.toggleFlag(SHOW_UNSUPPORTED) },
                        label = { Text(stringResource(R.string.unsupported)) },
                    )
                }
            }
        }
    }

    if (vm.compatibleVersions.isNotEmpty())
        UnsupportedDialog(
            appVersion = vm.appVersion,
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

    vm.pendingSelectionAction?.let {
        SelectionWarningDialog(
            onCancel = vm::dismissSelectionWarning,
            onConfirm = vm::confirmSelectionWarning
        )
    }

    fun LazyListScope.patchList(
        uid: Int,
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
                        vm.optionsDialog = uid to patch
                    },
                    selected = supported && vm.isSelected(
                        uid,
                        patch
                    ),
                    onToggle = {
                        if (vm.selectionWarningEnabled) {
                            vm.pendingSelectionAction = {
                                vm.togglePatch(uid, patch)
                            }
                        } else {
                            vm.togglePatch(uid, patch)
                        }
                    },
                    supported = supported
                )
            }
        }
    }

    search?.let { query ->
        SearchBar(
            query = query,
            onQueryChange = { new ->
                search = new
            },
            onSearch = {},
            active = true,
            onActiveChange = { new ->
                if (new) return@SearchBar
                search = null
            },
            placeholder = {
                Text(stringResource(R.string.search_patches))
            },
            leadingIcon = {
                IconButton(onClick = { search = null }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back)
                    )
                }
            }
        ) {
            val bundle = bundles[pagerState.currentPage]

            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize()
            ) {
                fun List<PatchInfo>.searched() = filter {
                    it.name.contains(query, true)
                }

                patchList(
                    uid = bundle.uid,
                    patches = bundle.supported.searched(),
                    filterFlag = SHOW_SUPPORTED,
                    supported = true
                )
                patchList(
                    uid = bundle.uid,
                    patches = bundle.universal.searched(),
                    filterFlag = SHOW_UNIVERSAL,
                    supported = true
                ) {
                    ListHeader(
                        title = stringResource(R.string.universal_patches),
                    )
                }

                if (!vm.allowExperimental) return@LazyColumnWithScrollbar
                patchList(
                    uid = bundle.uid,
                    patches = bundle.unsupported.searched(),
                    filterFlag = SHOW_UNSUPPORTED,
                    supported = true
                ) {
                    ListHeader(
                        title = stringResource(R.string.unsupported_patches),
                        onHelpClick = { vm.openUnsupportedDialog(bundle.unsupported) }
                    )
                }
            }
        }
    }


    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_patches),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = vm::reset) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                    }
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Outlined.FilterList, stringResource(R.string.more))
                    }
                    IconButton(
                        onClick = {
                            search = ""
                        }
                    ) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showPatchButton) return@Scaffold

            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.save)) },
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = {
                    // TODO: only allow this if all required options have been set.
                    onSave(vm.getCustomSelection(), vm.getOptions())
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
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->
                    val bundle = bundles[index]

                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.supported,
                            filterFlag = SHOW_SUPPORTED,
                            supported = true
                        )
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.universal,
                            filterFlag = SHOW_UNIVERSAL,
                            supported = true
                        ) {
                            ListHeader(
                                title = stringResource(R.string.universal_patches),
                            )
                        }
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.unsupported,
                            filterFlag = SHOW_UNSUPPORTED,
                            supported = vm.allowExperimental
                        ) {
                            ListHeader(
                                title = stringResource(R.string.unsupported_patches),
                                onHelpClick = { vm.openUnsupportedDialog(bundle.unsupported) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SelectionWarningDialog(
    onCancel: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val prefs: PreferencesManager = rememberKoinInject()
    var dismissPermanently by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            val enableCountdown by prefs.enableSelectionWarningCountdown.getAsState()

            Countdown(start = if (enableCountdown) 3 else 0) { timer ->
                LaunchedEffect(timer) {
                    if (timer == 0) prefs.enableSelectionWarningCountdown.update(false)
                }

                TextButton(
                    onClick = { onConfirm(dismissPermanently) },
                    enabled = timer == 0
                ) {
                    val text =
                        if (timer == 0) stringResource(R.string.continue_) else stringResource(
                            R.string.selection_warning_continue_countdown, timer
                        )
                    Text(text, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(Icons.Outlined.WarningAmber, null)
        },
        title = {
            Text(
                text = stringResource(R.string.selection_warning_title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.selection_warning_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.clickable {
                        dismissPermanently = !dismissPermanently
                    }
                ) {
                    Checkbox(
                        checked = dismissPermanently,
                        onCheckedChange = {
                            dismissPermanently = it
                        }
                    )
                    Text(stringResource(R.string.permanent_dismiss))
                }
            }
        }
    )
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
            onCheckedChange = { onToggle() },
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
                IconButton(onClick = it) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
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
        LazyColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (patch.options == null) return@LazyColumnWithScrollbar

            items(patch.options, key = { it.key }) { option ->
                val key = option.key
                val value =
                    if (values == null || !values.contains(key)) option.default else values[key]

                OptionItem(option = option, value = value, setValue = { set(key, it) })
            }
        }
    }
}