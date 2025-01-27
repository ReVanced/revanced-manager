package app.revanced.manager.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.CheckedFilterChip
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SafeguardDialog
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTab
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNSUPPORTED
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val (query, setQuery) = rememberSaveable {
        mutableStateOf("")
    }
    val (searchExpanded, setSearchExpanded) = rememberSaveable {
        mutableStateOf(false)
    }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val showSaveButton by remember {
        derivedStateOf { vm.selectionIsValid(bundles) }
    }

    val defaultPatchSelectionCount by vm.defaultSelectionCount
        .collectAsStateWithLifecycle(initialValue = 0)

    val selectedPatchCount by remember {
        derivedStateOf {
            vm.customPatchSelection?.values?.sumOf { it.size } ?: defaultPatchSelectionCount
        }
    }

    val patchLazyListStates = remember(bundles) { List(bundles.size) { LazyListState() } }

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

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CheckedFilterChip(
                        selected = vm.filter and SHOW_UNSUPPORTED == 0,
                        onClick = { vm.toggleFlag(SHOW_UNSUPPORTED) },
                        label = { Text(stringResource(R.string.supported)) }
                    )

                    CheckedFilterChip(
                        selected = vm.filter and SHOW_UNIVERSAL != 0,
                        onClick = { vm.toggleFlag(SHOW_UNIVERSAL) },
                        label = { Text(stringResource(R.string.universal)) },
                    )
                }
            }
        }
    }

    if (vm.compatibleVersions.isNotEmpty())
        UnsupportedPatchDialog(
            appVersion = vm.appVersion ?: stringResource(R.string.any_version),
            supportedVersions = vm.compatibleVersions,
            onDismissRequest = vm::dismissDialogs
        )
    var showUnsupportedPatchesDialog by rememberSaveable {
        mutableStateOf(false)
    }
    if (showUnsupportedPatchesDialog)
        UnsupportedPatchesDialog(
            appVersion = vm.appVersion ?: stringResource(R.string.any_version),
            onDismissRequest = { showUnsupportedPatchesDialog = false }
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

    var showSelectionWarning by rememberSaveable {
        mutableStateOf(false)
    }
    if (showSelectionWarning) {
        SelectionWarningDialog(onDismiss = { showSelectionWarning = false })
    }
    vm.pendingUniversalPatchAction?.let {
        UniversalPatchWarningDialog(
            onCancel = vm::dismissUniversalPatchWarning,
            onConfirm = vm::confirmUniversalPatchWarning
        )
    }

    fun LazyListScope.patchList(
        uid: Int,
        patches: List<PatchInfo>,
        visible: Boolean,
        supported: Boolean,
        header: (@Composable () -> Unit)? = null
    ) {
        if (patches.isNotEmpty() && visible) {
            header?.let {
                item(contentType = 0) {
                    it()
                }
            }

            items(
                items = patches,
                key = { it.name },
                contentType = { 1 }
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
                        when {
                            // Open unsupported dialog if the patch is not supported
                            !supported -> vm.openUnsupportedDialog(patch)

                            // Show selection warning if enabled
                            vm.selectionWarningEnabled -> showSelectionWarning = true

                            // Set pending universal patch action if the universal patch warning is enabled and there are no compatible packages
                            vm.universalPatchWarningEnabled && patch.compatiblePackages == null -> {
                                vm.pendingUniversalPatchAction = { vm.togglePatch(uid, patch) }
                            }

                            // Toggle the patch otherwise
                            else -> vm.togglePatch(uid, patch)
                        }
                    },
                    supported = supported
                )
            }
        }
    }

    Scaffold(
        topBar = {
            SearchBar(
                query = query,
                onQueryChange = setQuery,
                expanded = searchExpanded,
                onExpandedChange = setSearchExpanded,
                placeholder = {
                    Text(stringResource(R.string.search_patches))
                },
                leadingIcon = {
                    val rotation by animateFloatAsState(
                        targetValue = if (searchExpanded) 360f else 0f,
                        animationSpec = tween(durationMillis = 400, easing = EaseInOut),
                        label = "SearchBar back button"
                    )
                    IconButton(
                        onClick = {
                            if (searchExpanded) {
                                setSearchExpanded(false)
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier.rotate(rotation),
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                trailingIcon = {
                    AnimatedContent(
                        targetState = searchExpanded,
                        label = "Filter/Clear",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { searchExpanded ->
                        if (searchExpanded) {
                            IconButton(
                                onClick = { setQuery("") },
                                enabled = query.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        } else {
                            IconButton(onClick = { showBottomSheet = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                        }
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
                        visible = true,
                        supported = true
                    )
                    patchList(
                        uid = bundle.uid,
                        patches = bundle.universal.searched(),
                        visible = vm.filter and SHOW_UNIVERSAL != 0,
                        supported = true
                    ) {
                        ListHeader(
                            title = stringResource(R.string.universal_patches),
                        )
                    }

                    patchList(
                        uid = bundle.uid,
                        patches = bundle.unsupported.searched(),
                        visible = vm.filter and SHOW_UNSUPPORTED != 0,
                        supported = vm.allowIncompatiblePatches
                    ) {
                        ListHeader(
                            title = stringResource(R.string.unsupported_patches),
                            onHelpClick = { showUnsupportedPatchesDialog = true }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!showSaveButton) return@Scaffold

            AnimatedVisibility(
                visible = !searchExpanded,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = vm::reset,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                    }
                    HapticExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.save_with_count, selectedPatchCount)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.save)
                            )
                        },
                        expanded = patchLazyListStates.getOrNull(pagerState.currentPage)?.isScrollingUp ?: true,
                        onClick = {
                            onSave(vm.getCustomSelection(), vm.getOptions())
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (bundles.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
                ) {
                    bundles.forEachIndexed { index, bundle ->
                        HapticTab(
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
                    // Avoid crashing if the lists have not been fully initialized yet.
                    if (index > bundles.lastIndex || bundles.size != patchLazyListStates.size) return@HorizontalPager
                    val bundle = bundles[index]

                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        state = patchLazyListStates[index]
                    ) {
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.supported,
                            visible = true,
                            supported = true
                        )
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.universal,
                            visible = vm.filter and SHOW_UNIVERSAL != 0,
                            supported = true
                        ) {
                            ListHeader(
                                title = stringResource(R.string.universal_patches),
                            )
                        }
                        patchList(
                            uid = bundle.uid,
                            patches = bundle.unsupported,
                            visible = vm.filter and SHOW_UNSUPPORTED != 0,
                            supported = vm.allowIncompatiblePatches
                        ) {
                            ListHeader(
                                title = stringResource(R.string.unsupported_patches),
                                onHelpClick = { showUnsupportedPatchesDialog = true }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectionWarningDialog(onDismiss: () -> Unit) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.warning,
        body = stringResource(R.string.selection_warning_description),
    )
}

@Composable
private fun UniversalPatchWarningDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.continue_))
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
                text = stringResource(R.string.warning),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )
        },
        text = {
            Text(stringResource(R.string.universal_patch_warning_description))
        }
    )
}

@Composable
private fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    supported: Boolean = true
) = ListItem(
    modifier = Modifier
        .let { if (!supported) it.alpha(0.5f) else it }
        .clickable(onClick = onToggle)
        .fillMaxSize(),
    leadingContent = {
        HapticCheckbox(
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
    },
    colors = transparentListItemColors
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
        },
        colors = transparentListItemColors
    )
}

@Composable
private fun UnsupportedPatchesDialog(
    appVersion: String,
    onDismissRequest: () -> Unit
) = AlertDialog(
    icon = {
        Icon(Icons.Outlined.WarningAmber, null)
    },
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(stringResource(R.string.unsupported_patches)) },
    text = {
        Text(
            stringResource(
                R.string.unsupported_patches_dialog,
                appVersion
            )
        )
    }
)

@Composable
private fun UnsupportedPatchDialog(
    appVersion: String,
    supportedVersions: List<String>,
    onDismissRequest: () -> Unit
) = AlertDialog(
    icon = {
        Icon(Icons.Outlined.WarningAmber, null)
    },
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(stringResource(R.string.unsupported_patch)) },
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
private fun OptionsDialog(
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

                @Suppress("UNCHECKED_CAST")
                OptionItem(
                    option = option as Option<Any>,
                    value = value,
                    setValue = {
                        set(key, it)
                    }
                )
            }
        }
    }
}
