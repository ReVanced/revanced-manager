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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.CheckedFilterChip
import app.revanced.manager.ui.component.SafeguardDialog
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.bundle.BundlePatchList
import app.revanced.manager.ui.component.bundle.BundleSectionHeader
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.patches.IncompatiblePatchDialog
import app.revanced.manager.ui.component.patches.IncompatiblePatchesDialog
import app.revanced.manager.ui.component.patches.ListHeader
import app.revanced.manager.ui.component.patches.OptionsDialog
import app.revanced.manager.ui.component.patches.ScopeDialog
import app.revanced.manager.ui.component.patches.SelectionWarningDialog
import app.revanced.manager.ui.component.patches.UniversalPatchWarningDialog
import app.revanced.manager.ui.component.patches.patchList
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_INCOMPATIBLE
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun PatchesSelectorScreen(
    onSave: (PatchSelection?, Options) -> Unit,
    onBackClick: () -> Unit,
    viewModel: PatchesSelectorViewModel
) {
    val bundles by viewModel.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val (query, setQuery) = rememberSaveable { mutableStateOf("") }
    val (searchExpanded, setSearchExpanded) = rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val showSaveButton by remember { derivedStateOf { viewModel.selectionIsValid(bundles) } }

    val defaultPatchSelectionCount by viewModel.defaultSelectionCount
        .collectAsStateWithLifecycle(initialValue = 0)
    val selectedPatchCount by remember {
        derivedStateOf {
            viewModel.customPatchSelection?.values?.sumOf { it.size } ?: defaultPatchSelectionCount
        }
    }

    val lazyListState = rememberLazyListState()

    var showSelectionWarning by rememberSaveable { mutableStateOf(false) }
    var showUniversalWarning by rememberSaveable { mutableStateOf(false) }
    var showIncompatiblePatchesDialog by rememberSaveable { mutableStateOf(false) }
    var pendingScopeAction by remember { mutableStateOf<((Int?) -> Unit)?>(null) }

    fun executeScopedAction(action: (Int?) -> Unit) {
        if (bundles.size > 1) pendingScopeAction = action
        else action(bundles.firstOrNull()?.uid)
    }

    fun handlePatchClick(uid: Int, patch: PatchInfo, compatible: Boolean) {
        when {
            !compatible -> viewModel.openIncompatibleDialog(patch)
            viewModel.selectionWarningEnabled -> showSelectionWarning = true
            patch.compatiblePackages == null && viewModel.universalPatchWarningEnabled ->
                showUniversalWarning = true
            else -> viewModel.togglePatch(uid, patch)
        }
    }

    pendingScopeAction?.let { action ->
        val firstBundle = bundles.firstOrNull() ?: return@let
        ScopeDialog(
            bundleName = firstBundle.name,
            onDismissRequest = { pendingScopeAction = null },
            onAllPatches = { action(null); pendingScopeAction = null },
            onBundleOnly = { action(firstBundle.uid); pendingScopeAction = null }
        )
    }

    if (viewModel.compatibleVersions.isNotEmpty()) {
        IncompatiblePatchDialog(
            appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
            compatibleVersions = viewModel.compatibleVersions,
            onDismissRequest = viewModel::dismissDialogs
        )
    }

    if (showIncompatiblePatchesDialog) {
        IncompatiblePatchesDialog(
            appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
            onDismissRequest = { showIncompatiblePatchesDialog = false }
        )
    }

    viewModel.optionsDialog?.let { (bundle, patch) ->
        OptionsDialog(
            onDismissRequest = viewModel::dismissDialogs,
            patch = patch,
            values = viewModel.getOptions(bundle, patch),
            reset = { viewModel.resetOptions(bundle, patch) },
            set = { key, value -> viewModel.setOption(bundle, patch, key, value) },
            selectionWarningEnabled = viewModel.selectionWarningEnabled
        )
    }

    if (showSelectionWarning) SelectionWarningDialog(onDismiss = { showSelectionWarning = false })
    if (showUniversalWarning) UniversalPatchWarningDialog(onDismiss = { showUniversalWarning = false })

    if (showBottomSheet) {
        FilterAndActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            viewModel = viewModel,
            bundles = bundles,
            onShowSelectionWarning = { showSelectionWarning = true },
            executeScopedAction = ::executeScopedAction
        )
    }

    Scaffold(
        topBar = {
            PatchesSelectorSearchBar(
                query = query,
                onQueryChange = setQuery,
                searchExpanded = searchExpanded,
                onSearchExpandedChange = setSearchExpanded,
                onBackClick = onBackClick,
                onFilterClick = { showBottomSheet = true }
            ) {
                fun List<PatchInfo>.searched() = filter { it.name.contains(query, true) }

                BundlePatchList(
                    bundles = bundles,
                    uid = { it.uid },
                    modifier = Modifier.fillMaxSize(),
                    autoExpandInitial = true,
                    headerContent = { bundle, expanded, _ ->
                        if (bundles.size > 1) {
                            BundleSectionHeader(
                                name = bundle.name,
                                version = bundle.version,
                                expanded = expanded,
                            )
                        }
                    },
                    patchContent = { bundle ->
                        bundlePatchLists(
                            compatible = bundle.compatible.searched(),
                            universal = bundle.universal.searched(),
                            incompatible = bundle.incompatible.searched(),
                            filter = viewModel.filter,
                            allowIncompatiblePatches = viewModel.allowIncompatiblePatches,
                            onOptionsDialog = { patch -> viewModel.optionsDialog = bundle.uid to patch },
                            isSelected = { patch -> viewModel.isSelected(bundle.uid, patch) },
                            onPatchClick = { patch, compatible ->
                                handlePatchClick(bundle.uid, patch, compatible)
                            },
                            onIncompatibleHelpClick = { showIncompatiblePatchesDialog = true }
                        )
                    }
                )
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
                        onClick = viewModel::reset,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                    }

                    SaveButton(
                        lazyListState = lazyListState,
                        selectedPatchCount = selectedPatchCount,
                        onSave = { onSave(viewModel.getCustomSelection(), viewModel.getOptions()) }
                    )
                }
            }
        }
    ) { paddingValues ->
        BundlePatchList(
            bundles = bundles,
            uid = { it.uid },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            lazyListState = lazyListState,
            autoExpandInitial = true,
            headerContent = { bundle, expanded, onToggleExpand ->
                BundleSectionHeader(
                    name = bundle.name,
                    version = bundle.version,
                    expanded = expanded,
                    onToggleExpand = onToggleExpand,
                )
            },
            patchContent = { bundle ->
                bundlePatchLists(
                    compatible = bundle.compatible,
                    universal = bundle.universal,
                    incompatible = bundle.incompatible,
                    filter = viewModel.filter,
                    allowIncompatiblePatches = viewModel.allowIncompatiblePatches,
                    onOptionsDialog = { patch -> viewModel.optionsDialog = bundle.uid to patch },
                    isSelected = { patch -> viewModel.isSelected(bundle.uid, patch) },
                    onPatchClick = { patch, compatible ->
                        handlePatchClick(bundle.uid, patch, compatible)
                    },
                    onIncompatibleHelpClick = { showIncompatiblePatchesDialog = true }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatchesSelectorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onFilterClick: () -> Unit,
    searchResultsContent: @Composable () -> Unit
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        expanded = searchExpanded,
        onExpandedChange = onSearchExpandedChange,
        placeholder = { Text(stringResource(R.string.search_patches)) },
        leadingIcon = {
            SearchBarBackButton(
                searchExpanded = searchExpanded,
                onSearchCollapse = { onSearchExpandedChange(false) },
                onBackClick = onBackClick
            )
        },
        trailingIcon = {
            SearchBarTrailingActions(
                searchExpanded = searchExpanded,
                query = query,
                onClearQuery = { onQueryChange("") },
                onFilterClick = onFilterClick
            )
        }
    ) {
        searchResultsContent()
    }
}

@Composable
private fun SearchBarBackButton(
    searchExpanded: Boolean,
    onSearchCollapse: () -> Unit,
    onBackClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (searchExpanded) 360f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseInOut),
        label = "SearchBar back button"
    )
    IconButton(
        onClick = { if (searchExpanded) onSearchCollapse() else onBackClick() }
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
private fun SearchBarTrailingActions(
    searchExpanded: Boolean,
    query: String,
    onClearQuery: () -> Unit,
    onFilterClick: () -> Unit
) {
    AnimatedContent(
        targetState = searchExpanded,
        label = "Filter/Clear",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { expanded ->
        if (expanded) {
            IconButton(onClick = onClearQuery, enabled = query.isNotEmpty()) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
            }
        } else {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.more))
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun SaveButton(
    lazyListState: LazyListState,
    selectedPatchCount: Int,
    onSave: () -> Unit
) {
    val isScrollingUp = lazyListState.isScrollingUp()
    val expanded by produceState(true, isScrollingUp) {
        value = isScrollingUp.value
        snapshotFlow { isScrollingUp.value }
            .sample(333L)
            .collect { value = it }
    }

    HapticExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.save_with_count, selectedPatchCount)) },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = stringResource(R.string.save)
            )
        },
        expanded = expanded,
        onClick = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterAndActionsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: PatchesSelectorViewModel,
    bundles: List<PatchBundleInfo.Scoped>,
    onShowSelectionWarning: () -> Unit,
    executeScopedAction: ((Int?) -> Unit) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            FilterSection(
                filter = viewModel.filter,
                onToggleFlag = viewModel::toggleFlag
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            ActionsSection(
                viewModel = viewModel,
                bundles = bundles,
                onDismiss = onDismiss,
                onShowSelectionWarning = onShowSelectionWarning,
                executeScopedAction = executeScopedAction
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    filter: Int,
    onToggleFlag: (Int) -> Unit
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
            selected = filter and SHOW_INCOMPATIBLE == 0,
            onClick = { onToggleFlag(SHOW_INCOMPATIBLE) },
            label = { Text(stringResource(R.string.this_version)) }
        )
        CheckedFilterChip(
            selected = filter and SHOW_UNIVERSAL != 0,
            onClick = { onToggleFlag(SHOW_UNIVERSAL) },
            label = { Text(stringResource(R.string.universal)) }
        )
    }
}

@Composable
private fun ActionsSection(
    viewModel: PatchesSelectorViewModel,
    bundles: List<PatchBundleInfo.Scoped>,
    onDismiss: () -> Unit,
    onShowSelectionWarning: () -> Unit,
    executeScopedAction: ((Int?) -> Unit) -> Unit
) {
    Text(
        text = stringResource(R.string.patch_selector_sheet_actions_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    fun guardedAction(action: () -> Unit) {
        onDismiss()
        if (viewModel.selectionWarningEnabled) {
            onShowSelectionWarning()
        } else {
            action()
        }
    }

    ActionItem(
        icon = Icons.Outlined.Restore,
        text = stringResource(R.string.restore_default_selection),
        onClick = {
            guardedAction {
                executeScopedAction { uid -> viewModel.restoreDefaults(uid) }
            }
        }
    )

    ActionItem(
        icon = Icons.Outlined.Deselect,
        text = stringResource(R.string.deselect_all),
        onClick = {
            guardedAction {
                executeScopedAction { uid -> viewModel.deselectAll(bundles, uid) }
            }
        }
    )

    ActionItem(
        icon = Icons.Outlined.SwapHoriz,
        text = stringResource(R.string.invert_selection),
        onClick = {
            guardedAction {
                executeScopedAction { uid -> viewModel.invertSelection(bundles, uid) }
            }
        }
    )
}

private fun LazyListScope.bundlePatchLists(
    compatible: List<PatchInfo>,
    universal: List<PatchInfo>,
    incompatible: List<PatchInfo>,
    filter: Int,
    allowIncompatiblePatches: Boolean,
    onOptionsDialog: (PatchInfo) -> Unit,
    isSelected: (PatchInfo) -> Boolean,
    onPatchClick: (PatchInfo, Boolean) -> Unit,
    onIncompatibleHelpClick: () -> Unit
) {
    patchList(
        patches = compatible,
        visible = true,
        compatible = true,
        onOptionsDialog = onOptionsDialog,
        isSelected = isSelected,
        onPatchClick = onPatchClick
    )
    patchList(
        patches = universal,
        visible = filter and SHOW_UNIVERSAL != 0,
        compatible = true,
        onOptionsDialog = onOptionsDialog,
        isSelected = isSelected,
        onPatchClick = onPatchClick,
        header = { ListHeader(title = stringResource(R.string.universal_patches)) }
    )
    patchList(
        patches = incompatible,
        visible = filter and SHOW_INCOMPATIBLE != 0,
        compatible = allowIncompatiblePatches,
        onOptionsDialog = onOptionsDialog,
        isSelected = isSelected,
        onPatchClick = onPatchClick,
        header = {
            ListHeader(
                title = stringResource(R.string.incompatible_patches),
                onHelpClick = onIncompatibleHelpClick
            )
        }
    )
}

@Composable
fun ActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(text) },
        colors = transparentListItemColors
    )
}