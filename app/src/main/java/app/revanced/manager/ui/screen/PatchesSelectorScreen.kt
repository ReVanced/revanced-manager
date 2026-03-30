package app.revanced.manager.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.TooltipHost
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.patches.BundleSection
import app.revanced.manager.ui.component.patches.IncompatiblePatchDialog
import app.revanced.manager.ui.component.patches.IncompatiblePatchesDialog
import app.revanced.manager.ui.component.patches.OptionsDialog
import app.revanced.manager.ui.component.patches.PatchItem
import app.revanced.manager.ui.component.patches.PatchesFilterBottomSheet
import app.revanced.manager.ui.component.patches.PatchesListHeader
import app.revanced.manager.ui.component.patches.SelectionWarningDialog
import app.revanced.manager.ui.component.patches.SourceSectionHeader
import app.revanced.manager.ui.component.patches.UniversalPatchWarningDialog
import app.revanced.manager.ui.component.patches.buildBundleSections
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.DialogState
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.isScrollingUp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun PatchesSelectorScreen(
    onSave: (PatchSelection?, Options) -> Unit,
    onBackClick: () -> Unit,
    onBundleInfoClick: (Int) -> Unit,
    isSourceEditMode: Boolean = false,
    onSourceDeleteRequest: ((Int) -> Unit)? = null,
    viewModel: PatchesSelectorViewModel
) {
    val stickyHeaderTopGap = 8.dp
    val readOnly = viewModel.readOnly
    val bundles by viewModel.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val bundleLoadIssues by viewModel.bundleLoadIssuesFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val patchLazyListState = rememberLazyListState()
    val searchLazyListState = rememberLazyListState()
    val (query, setQuery) = rememberSaveable { mutableStateOf("") }
    val (searchExpanded, setSearchExpanded) = rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var collapsedBundleUids by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var selectedPackageFilters by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val effectiveSelectedPackageFilters = if (readOnly) selectedPackageFilters else emptySet()

    val showSaveButton by remember {
        derivedStateOf { !readOnly && viewModel.selectionIsValid(bundles) }
    }

    val defaultPatchSelectionCount by viewModel.defaultSelectionCount
        .collectAsStateWithLifecycle(initialValue = 0)

    val selectedPatchCount by remember {
        derivedStateOf {
            viewModel.customPatchSelection?.values?.sumOf { it.size } ?: defaultPatchSelectionCount
        }
    }


    fun toggleBundleExpanded(bundleUid: Int) {
        if (isSourceEditMode) return
        collapsedBundleUids = if (bundleUid in collapsedBundleUids) {
            collapsedBundleUids - bundleUid
        } else {
            collapsedBundleUids + bundleUid
        }
    }

    fun onBundleSelectionClick(bundle: PatchBundleInfo.Scoped) {
        if (readOnly) return

        val selectionState = viewModel.getBundleSelectionState(bundle)
        when {
            viewModel.selectionWarningEnabled -> viewModel.showSelectionWarning()
            selectionState == false -> viewModel.restoreDefaults(bundle.uid)
            else -> viewModel.deselectAll(bundles, bundle.uid)
        }
    }

    val effectiveCollapsedBundleUids =
        remember(bundles, collapsedBundleUids, readOnly, isSourceEditMode) {
            when {
                isSourceEditMode -> bundles.map { it.uid }
                readOnly -> bundles.map { it.uid }.filter { it !in collapsedBundleUids }
                else -> collapsedBundleUids
            }
        }

    val sections = remember(
        bundles,
        viewModel.filter,
        effectiveCollapsedBundleUids,
        effectiveSelectedPackageFilters
    ) {
        buildBundleSections(
            bundles = bundles,
            filter = viewModel.filter,
            collapsedBundleUids = effectiveCollapsedBundleUids,
            selectedPackageNames = effectiveSelectedPackageFilters
        )
    }
    val searchSections = remember(
        bundles,
        query,
        viewModel.filter,
        effectiveCollapsedBundleUids,
        effectiveSelectedPackageFilters
    ) {
        buildBundleSections(
            bundles = bundles,
            query = query,
            filter = viewModel.filter,
            collapsedBundleUids = effectiveCollapsedBundleUids,
            selectedPackageNames = effectiveSelectedPackageFilters,
            forceExpanded = query.isNotBlank()
        )
    }

    if (showBottomSheet) {
        PatchesFilterBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sections = sections,
            patchLazyListState = patchLazyListState,
            bundles = bundles,
            filter = viewModel.filter,
            onToggleFlag = viewModel::toggleFlag,
            packageName = viewModel.packageName.ifBlank { null },
            readOnly = readOnly,
            selectionWarningEnabled = viewModel.selectionWarningEnabled,
            onShowSelectionWarning = viewModel::showSelectionWarning,
            onRestoreDefaults = viewModel::restoreDefaults,
            onDeselectAll = { uid -> viewModel.deselectAll(bundles, uid) },
            onInvertSelection = { uid -> viewModel.invertSelection(bundles, uid) },
            onDeselectAllExcept = viewModel::deselectAllExcept,
            selectedPackageFilters = selectedPackageFilters,
            onTogglePackageFilter = { pkg ->
                selectedPackageFilters = if (pkg in selectedPackageFilters) {
                    selectedPackageFilters - pkg
                } else {
                    selectedPackageFilters + pkg
                }
            }
        )
    }

    when (val dialog = viewModel.activeDialog) {
        is DialogState.IncompatiblePatch -> {
            IncompatiblePatchDialog(
                appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
                compatibleVersions = dialog.compatibleVersions,
                onDismissRequest = viewModel::dismissDialogs
            )
        }

        is DialogState.IncompatiblePatchesInfo -> {
            IncompatiblePatchesDialog(
                appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
                onDismissRequest = viewModel::dismissDialogs
            )
        }

        is DialogState.Options -> {
            OptionsDialog(
                onDismissRequest = viewModel::dismissDialogs,
                patch = dialog.patch,
                values = viewModel.getOptions(dialog.bundle, dialog.patch),
                reset = { viewModel.resetOptions(dialog.bundle, dialog.patch) },
                resetOption = { viewModel.resetOption(dialog.bundle, dialog.patch, it) },
                set = { key, value ->
                    viewModel.setOption(
                        dialog.bundle,
                        dialog.patch,
                        key,
                        value
                    )
                },
                selectionWarningEnabled = viewModel.selectionWarningEnabled,
                readOnly = readOnly,
            )
        }

        is DialogState.SelectionWarning -> {
            SelectionWarningDialog(onDismiss = viewModel::dismissDialogs)
        }

        is DialogState.UniversalPatchWarning -> {
            UniversalPatchWarningDialog(onDismiss = viewModel::dismissDialogs)
        }

        else -> {}
    }

    fun LazyListScope.patchList(
        uid: Int,
        patches: List<PatchInfo>,
        compatible: Boolean,
        keyPrefix: String,
        header: (@Composable () -> Unit)? = null
    ) {
        if (patches.isEmpty()) return

        header?.let {
            item(key = "$keyPrefix-header", contentType = 0) { it() }
        }

        itemsIndexed(
            items = patches,
            key = { index, patch -> patchItemKey(keyPrefix, patch.name, index) },
            contentType = { _, _ -> 1 }
        ) { _, patch ->
            PatchItem(
                patch = patch,
                onOptionsDialog = { viewModel.openOptionsDialog(uid, patch) },
                selected = compatible && viewModel.isSelected(uid, patch),
                onToggle = {
                    when {
                        !compatible -> viewModel.openIncompatibleDialog(patch)
                        viewModel.selectionWarningEnabled -> viewModel.showSelectionWarning()
                        patch.compatiblePackages == null && viewModel.universalPatchWarningEnabled ->
                            viewModel.showUniversalPatchWarning()

                        else -> viewModel.togglePatch(uid, patch)
                    }
                },
                compatible = compatible,
                readOnly = readOnly,
                scopedPackageName = viewModel.packageName.ifBlank { null }
            )
        }
    }

    fun LazyListScope.sectionedPatchList(
        sections: List<BundleSection>,
        keyPrefix: String
    ) {
        sections.forEach { section ->
            val bundle = section.bundle
            val loadIssueResId = bundleLoadIssues[bundle.uid]

            stickyHeader(key = "$keyPrefix-source-${bundle.uid}") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    SourceSectionHeader(
                        bundle = bundle,
                        expanded = section.expanded,
                        selectionState = viewModel.getBundleSelectionState(bundle),
                        onClick = { onBundleInfoClick(bundle.uid) },
                        onSelectionClick = { onBundleSelectionClick(bundle) },
                        onExpandToggle = { toggleBundleExpanded(bundle.uid) },
                        onDeleteClick = { onSourceDeleteRequest?.invoke(bundle.uid) },
                        sourceEditMode = isSourceEditMode,
                        readOnly = readOnly,
                        loadIssue = loadIssueResId?.let { messageId ->
                            stringResource(messageId)
                        }
                    )
                }
            }

            if (!section.expanded) return@forEach

            patchList(
                uid = bundle.uid,
                patches = section.compatible,
                compatible = true,
                keyPrefix = "$keyPrefix-compatible-${bundle.uid}"
            )

            patchList(
                uid = bundle.uid,
                patches = section.universal,
                compatible = true,
                keyPrefix = "$keyPrefix-universal-${bundle.uid}"
            ) {
                PatchesListHeader(title = stringResource(R.string.universal_patches))
            }

            patchList(
                uid = bundle.uid,
                patches = section.incompatible,
                compatible = viewModel.allowIncompatiblePatches,
                keyPrefix = "$keyPrefix-incompatible-${bundle.uid}"
            ) {
                PatchesListHeader(
                    title = stringResource(R.string.incompatible_patches),
                    onHelpClick = { viewModel.showIncompatiblePatchesInfo() }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(horizontal = if (searchExpanded) 0.dp else 16.dp)) {
                SearchBar(
                    query = query,
                    onQueryChange = setQuery,
                    expanded = searchExpanded,
                    onExpandedChange = setSearchExpanded,
                    placeholder = { Text(stringResource(R.string.search_patches)) },
                    windowInsets = if (readOnly) WindowInsets(
                        0,
                        0,
                        0,
                        0
                    ) else WindowInsets.systemBars,
                    leadingIcon = {
                        TooltipIconButton(
                            onClick = {
                                if (searchExpanded) setSearchExpanded(false) else onBackClick()
                            },
                            tooltip = stringResource(R.string.back),
                        ) { contentDescription ->
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = contentDescription
                            )
                        }
                    },
                    trailingIcon = {
                        AnimatedContent(
                            targetState = searchExpanded,
                            label = "Filter/Clear",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { expanded ->
                            if (expanded) {
                                TooltipIconButton(
                                    onClick = { setQuery("") },
                                    enabled = query.isNotEmpty(),
                                    tooltip = stringResource(R.string.clear),
                                ) { contentDescription ->
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = contentDescription
                                    )
                                }
                            } else {
                                TooltipIconButton(
                                    onClick = { showBottomSheet = true },
                                    tooltip = stringResource(R.string.more),
                                ) { contentDescription ->
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = contentDescription
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumnWithScrollbar(
                            modifier = Modifier.fillMaxSize(),
                            state = searchLazyListState,
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            sectionedPatchList(
                                sections = searchSections,
                                keyPrefix = "search"
                            )
                        }
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
                    TooltipHost(tooltip = stringResource(R.string.reset)) { tooltipModifier ->
                        SmallFloatingActionButton(
                            onClick = viewModel::reset,
                            modifier = tooltipModifier,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                        }
                    }

                    val isScrollingUp = patchLazyListState.isScrollingUp()
                    val expanded by produceState(true, isScrollingUp) {
                        value = isScrollingUp.value
                        snapshotFlow { isScrollingUp.value }
                            .sample(333L)
                            .collect { value = it }
                    }

                    HapticExtendedFloatingActionButton(
                        text = {
                            Text(stringResource(R.string.save_with_count, selectedPatchCount))
                        },
                        tooltip = stringResource(R.string.save),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.save)
                            )
                        },
                        expanded = expanded,
                        onClick = {
                            onSave(viewModel.getCustomSelection(), viewModel.getOptions())
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (searchExpanded) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stickyHeaderTopGap)
                    .background(MaterialTheme.colorScheme.surface)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumnWithScrollbar(
                    modifier = Modifier.fillMaxSize(),
                    state = patchLazyListState
                ) {
                    sectionedPatchList(
                        sections = sections,
                        keyPrefix = "main"
                    )
                }
            }
        }
    }
}

private fun patchItemKey(keyPrefix: String, patchName: String, index: Int) =
    "$keyPrefix-$index-$patchName"