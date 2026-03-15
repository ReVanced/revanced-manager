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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.CheckedFilterChip
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.SafeguardDialog
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTriStateCheckbox
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.component.patches.SelectionWarningDialog
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_INCOMPATIBLE
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.PM
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.transparentListItemColors
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    FlowPreview::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
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
    val pm: PM = koinInject()
    val pmAppList by pm.appList.collectAsStateWithLifecycle(initialValue = emptyList())
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

    var showSelectionWarning by rememberSaveable { mutableStateOf(false) }
    var showUniversalWarning by rememberSaveable { mutableStateOf(false) }
    var pendingScopeAction by remember { mutableStateOf<((Int?) -> Unit)?>(null) }

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
            viewModel.selectionWarningEnabled -> showSelectionWarning = true
            selectionState == false -> viewModel.restoreDefaults(bundle.uid)
            else -> viewModel.deselectAll(bundles, bundle.uid)
        }
    }

    val installedPackageLabels = remember(pmAppList) {
        pmAppList.filter { it.packageInfo != null }
            .associate { appInfo ->
                appInfo.packageName to pm.run { appInfo.packageInfo!!.label() }
            }
    }

    val packagePatchCounts = remember(bundles) {
        bundles.asSequence()
            .flatMap { it.patches.asSequence() }
            .flatMap { it.compatiblePackages.orEmpty().asSequence() }
            .groupingBy { it.packageName }
            .eachCount()
    }

    val sections = remember(
        bundles,
        viewModel.filter,
        collapsedBundleUids,
        isSourceEditMode,
        effectiveSelectedPackageFilters
    ) {
        buildBundleSections(
            bundles = bundles,
            filter = viewModel.filter,
            collapsedBundleUids = if (isSourceEditMode) bundles.map { it.uid } else collapsedBundleUids,
            selectedPackageNames = effectiveSelectedPackageFilters
        )
    }
    val searchSections = remember(
        bundles,
        query,
        viewModel.filter,
        collapsedBundleUids,
        isSourceEditMode,
        effectiveSelectedPackageFilters
    ) {
        buildBundleSections(
            bundles = bundles,
            query = query,
            filter = viewModel.filter,
            collapsedBundleUids = if (isSourceEditMode) bundles.map { it.uid } else collapsedBundleUids,
            selectedPackageNames = effectiveSelectedPackageFilters,
            forceExpanded = query.isNotBlank()
        )
    }

    val sectionLayouts = remember(sections) { buildSectionLayouts(sections) }
    val currentBundle by remember(sectionLayouts, patchLazyListState) {
        derivedStateOf {
            sectionLayouts.lastOrNull { it.headerIndex <= patchLazyListState.firstVisibleItemIndex }?.bundle
                ?: bundles.firstOrNull()
        }
    }

    fun executeScopedAction(action: (Int?) -> Unit) {
        if (bundles.size > 1) {
            pendingScopeAction = action
        } else {
            action(bundles.firstOrNull()?.uid)
        }
    }

    pendingScopeAction?.let { action ->
        val activeBundle = currentBundle ?: return@let

        ScopeDialog(
            bundleName = activeBundle.name,
            onDismissRequest = { pendingScopeAction = null },
            onAllPatches = {
                action(null)
                pendingScopeAction = null
            },
            onBundleOnly = {
                action(activeBundle.uid)
                pendingScopeAction = null
            }
        )
    }

    if (showBottomSheet) {
        val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = modalBottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.patch_selector_sheet_filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.patch_selector_sheet_filter_compat_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.packageName.isNotBlank()) {
                            CheckedFilterChip(
                                selected = viewModel.filter and SHOW_INCOMPATIBLE == 0,
                                onClick = { viewModel.toggleFlag(SHOW_INCOMPATIBLE) },
                                label = { Text(stringResource(R.string.this_version)) }
                            )
                        }

                        CheckedFilterChip(
                            selected = viewModel.filter and SHOW_UNIVERSAL != 0,
                            onClick = { viewModel.toggleFlag(SHOW_UNIVERSAL) },
                            label = { Text(stringResource(R.string.universal)) }
                        )
                    }

                    if (readOnly && packagePatchCounts.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.patch_selector_sheet_filter_packages_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            packagePatchCounts
                                .toList()
                                .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
                                .forEach { (packageName, count) ->
                                    val label = installedPackageLabels[packageName] ?: packageName
                                    CheckedFilterChip(
                                        selected = packageName in selectedPackageFilters,
                                        onClick = {
                                            selectedPackageFilters = if (packageName in selectedPackageFilters) {
                                                selectedPackageFilters - packageName
                                            } else {
                                                selectedPackageFilters + packageName
                                            }
                                        },
                                        label = { Text("$label ($count)") }
                                    )
                                }
                        }
                    }
                }

                fun guardedAction(action: () -> Unit) {
                    showBottomSheet = false
                    if (viewModel.selectionWarningEnabled) {
                        showSelectionWarning = true
                    } else {
                        action()
                    }
                }

                if (!readOnly) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Text(
                        text = stringResource(R.string.patch_selector_sheet_actions_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    ListSection(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        ActionItem(
                            icon = Icons.Outlined.Restore,
                            text = stringResource(R.string.restore_default_selection),
                            onClick = {
                                guardedAction {
                                    executeScopedAction { uid ->
                                        viewModel.restoreDefaults(uid)
                                    }
                                }
                            }
                        )

                        ActionItem(
                            icon = Icons.Outlined.Deselect,
                            text = stringResource(R.string.deselect_all),
                            onClick = {
                                guardedAction {
                                    executeScopedAction { uid ->
                                        viewModel.deselectAll(bundles, uid)
                                    }
                                }
                            }
                        )

                        ActionItem(
                            icon = Icons.Outlined.SwapHoriz,
                            text = stringResource(R.string.invert_selection),
                            onClick = {
                                guardedAction {
                                    executeScopedAction { uid ->
                                        viewModel.invertSelection(bundles, uid)
                                    }
                                }
                            }
                        )

                        currentBundle?.let { bundle ->
                            ActionItem(
                                icon = Icons.Outlined.Deselect,
                                text = stringResource(R.string.deselect_all_except, bundle.name),
                                onClick = {
                                    guardedAction {
                                        viewModel.deselectAllExcept(bundles, bundle.uid)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (viewModel.compatibleVersions.isNotEmpty() && !readOnly) {
        IncompatiblePatchDialog(
            appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
            compatibleVersions = viewModel.compatibleVersions,
            onDismissRequest = viewModel::dismissDialogs
        )
    }

    var showIncompatiblePatchesDialog by rememberSaveable { mutableStateOf(false) }
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
            selectionWarningEnabled = viewModel.selectionWarningEnabled,
            readOnly = readOnly
        )
    }

    if (showSelectionWarning) {
        SelectionWarningDialog(onDismiss = { showSelectionWarning = false })
    }

    if (showUniversalWarning) {
        UniversalPatchWarningDialog(onDismiss = { showUniversalWarning = false })
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
                onOptionsDialog = { viewModel.optionsDialog = uid to patch },
                selected = compatible && viewModel.isSelected(uid, patch),
                onToggle = {
                    when {
                        !compatible -> viewModel.openIncompatibleDialog(patch)
                        viewModel.selectionWarningEnabled -> showSelectionWarning = true
                        patch.compatiblePackages == null && viewModel.universalPatchWarningEnabled -> showUniversalWarning = true
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
                ListHeader(title = stringResource(R.string.universal_patches))
            }

            patchList(
                uid = bundle.uid,
                patches = section.incompatible,
                compatible = viewModel.allowIncompatiblePatches,
                keyPrefix = "$keyPrefix-incompatible-${bundle.uid}"
            ) {
                ListHeader(
                    title = stringResource(R.string.incompatible_patches),
                    onHelpClick = { showIncompatiblePatchesDialog = true }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SearchBar(
                    query = query,
                    onQueryChange = setQuery,
                    expanded = searchExpanded,
                    onExpandedChange = setSearchExpanded,
                    placeholder = { Text(stringResource(R.string.search_patches)) },
                    windowInsets = if (readOnly) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (searchExpanded) setSearchExpanded(false) else onBackClick()
                            },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
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
                        ) { expanded ->
                            if (expanded) {
                                IconButton(
                                    onClick = { setQuery("") },
                                    enabled = query.isNotEmpty(),
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { showBottomSheet = true },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = stringResource(R.string.more)
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
                    SmallFloatingActionButton(
                        onClick = viewModel::reset,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
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
                .padding(paddingValues)
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
                    state = patchLazyListState,
                    contentPadding = PaddingValues(bottom = 16.dp)
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

@Composable
private fun UniversalPatchWarningDialog(
    onDismiss: () -> Unit
) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.warning,
        body = stringResource(R.string.universal_patch_warning_description),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    compatible: Boolean = true,
    readOnly: Boolean = false,
    scopedPackageName: String? = null
) {
    val anyVersionLabel = stringResource(R.string.patches_view_any_version)
    val anyAppLabel = stringResource(R.string.universal)

    val chipLabels = remember(patch.compatiblePackages, scopedPackageName, anyVersionLabel, anyAppLabel) {
        val pkgs = patch.compatiblePackages
        when {
            pkgs == null -> if (scopedPackageName == null) listOf(anyAppLabel) else emptyList()
            scopedPackageName != null -> {
                val pkg = pkgs.firstOrNull { it.packageName == scopedPackageName }
                    ?: return@remember emptyList()
                val versions = pkg.versions
                if (versions.isNullOrEmpty()) listOf(anyVersionLabel) else versions.toList()
            }
            else -> {
                pkgs.map { pkg ->
                    val versions = pkg.versions
                    if (versions.isNullOrEmpty()) {
                        "${pkg.packageName} ($anyVersionLabel)"
                    } else {
                        "${pkg.packageName} (${versions.joinToString(", ")})"
                    }
                }
            }
        }
    }

    ListItem(
        modifier = Modifier
            .let { if (!compatible) it.alpha(0.5f) else it }
            .clickable(enabled = !readOnly, onClick = onToggle)
            .fillMaxSize(),
        leadingContent = {
            HapticCheckbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = compatible && !readOnly
            )
        },
        headlineContent = { Text(patch.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                patch.description?.let { Text(it) }
                if (chipLabels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chipLabels.forEach { label ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        trailingContent = {
            if (patch.options?.isNotEmpty() == true) {
                IconButton(onClick = onOptionsDialog, enabled = compatible || readOnly, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        },
        colors = transparentListItemColors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                IconButton(onClick = it, shapes = IconButtonDefaults.shapes()) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IncompatiblePatchesDialog(
    appVersion: String,
    onDismissRequest: () -> Unit
) = AlertDialog(
    icon = {
        Icon(Icons.Outlined.WarningAmber, null)
    },
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(stringResource(R.string.incompatible_patches)) },
    text = {
        Text(
            stringResource(
                R.string.incompatible_patches_dialog,
                appVersion
            )
        )
    }
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IncompatiblePatchDialog(
    appVersion: String,
    compatibleVersions: List<String>,
    onDismissRequest: () -> Unit
) = AlertDialog(
    icon = {
        Icon(Icons.Outlined.WarningAmber, null)
    },
    onDismissRequest = onDismissRequest,
    confirmButton = {
        TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.ok))
        }
    },
    title = { Text(stringResource(R.string.incompatible_patch)) },
    text = {
        Text(
            stringResource(
                R.string.app_version_not_compatible,
                appVersion,
                compatibleVersions.joinToString(", ")
            )
        )
    }
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = { Icon(icon, contentDescription = null) },
    ) { Text(text) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScopeDialog(
    bundleName: String,
    onDismissRequest: () -> Unit,
    onAllPatches: () -> Unit,
    onBundleOnly: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.scope_dialog_title)) },
    confirmButton = {
        TextButton(onClick = onAllPatches, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.scope_all_patches))
        }
    },
    dismissButton = {
        TextButton(onClick = onBundleOnly, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.scope_bundle_patches, bundleName))
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OptionsDialog(
    patch: PatchInfo,
    values: Map<String, Any?>?,
    reset: () -> Unit,
    set: (String, Any?) -> Unit,
    onDismissRequest: () -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean
) = FullscreenDialog(onDismissRequest = onDismissRequest) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = patch.name,
                onBackClick = onDismissRequest,
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = reset, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Filled.Restore, stringResource(R.string.reset))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (patch.options == null) return@LazyColumnWithScrollbar

            items(patch.options, key = { it.name }) { option ->
                val name = option.name
                val value =
                    if (values == null || !values.contains(name)) option.default else values[name]

                @Suppress("UNCHECKED_CAST")
                OptionItem(
                    option = option as Option<Any>,
                    value = value,
                    setValue = {
                        set(name, it)
                    },
                    selectionWarningEnabled = selectionWarningEnabled,
                    readOnly = readOnly
                )
            }
        }
    }
}

private data class BundleSection(
    val bundle: PatchBundleInfo.Scoped,
    val compatible: List<PatchInfo>,
    val universal: List<PatchInfo>,
    val incompatible: List<PatchInfo>,
    val expanded: Boolean
) {
    val hasVisiblePatches: Boolean
        get() = compatible.isNotEmpty() || universal.isNotEmpty() || incompatible.isNotEmpty()
}

private data class BundleSectionLayout(
    val bundle: PatchBundleInfo.Scoped,
    val headerIndex: Int
)

private fun buildBundleSections(
    bundles: List<PatchBundleInfo.Scoped>,
    query: String = "",
    filter: Int,
    collapsedBundleUids: List<Int>,
    selectedPackageNames: Set<String> = emptySet(),
    forceExpanded: Boolean = false
): List<BundleSection> {
    fun PatchInfo.matchesPackageFilter(): Boolean {
        if (selectedPackageNames.isEmpty()) return true
        val packages = compatiblePackages ?: return true
        return packages.any { it.packageName in selectedPackageNames }
    }

    fun PatchInfo.matchesSearchQuery(): Boolean {
        if (query.isBlank()) return true

        return name.contains(query, ignoreCase = true) ||
            description?.contains(query, ignoreCase = true) == true ||
            compatiblePackages?.any { pkg ->
                pkg.packageName.contains(query, ignoreCase = true)
            } == true
    }

    fun List<PatchInfo>.searched() = if (query.isBlank()) {
        filter { it.matchesPackageFilter() }
    } else {
        filter { it.matchesSearchQuery() && it.matchesPackageFilter() }
    }

    return bundles.mapNotNull { bundle ->
        BundleSection(
            bundle = bundle,
            compatible = bundle.compatible.searched(),
            universal = if (filter and SHOW_UNIVERSAL != 0) bundle.universal.searched() else emptyList(),
            incompatible = if (filter and SHOW_INCOMPATIBLE != 0) bundle.incompatible.searched() else emptyList(),
            expanded = forceExpanded || bundle.uid !in collapsedBundleUids
        ).takeIf { query.isBlank() || it.hasVisiblePatches }
    }
}

private fun buildSectionLayouts(sections: List<BundleSection>) = buildList {
    var itemIndex = 0

    sections.forEach { section ->
        add(BundleSectionLayout(section.bundle, itemIndex))
        itemIndex += 1

        if (!section.expanded) return@forEach

        itemIndex += section.compatible.size
        if (section.universal.isNotEmpty()) {
            itemIndex += 1 + section.universal.size
        }
        if (section.incompatible.isNotEmpty()) {
            itemIndex += 1 + section.incompatible.size
        }
    }
}

private fun patchItemKey(keyPrefix: String, patchName: String, index: Int) =
    "$keyPrefix-$index-$patchName"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceSectionHeader(
    bundle: PatchBundleInfo.Scoped,
    expanded: Boolean,
    selectionState: Boolean?,
    onClick: () -> Unit,
    onSelectionClick: () -> Unit,
    onExpandToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    sourceEditMode: Boolean,
    readOnly: Boolean,
    loadIssue: String?
) {
    val toggleableState = when (selectionState) {
        true -> ToggleableState.On
        false -> ToggleableState.Off
        null -> ToggleableState.Indeterminate
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(durationMillis = 250, easing = EaseInOut),
        label = "Bundle section expand state"
    )

    Column {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            leadingContent = {
                HapticTriStateCheckbox(
                    state = toggleableState,
                    onClick = onSelectionClick,
                    enabled = !readOnly
                )
            },
            headlineContent = {
                Text(text = bundle.name)
            },
            supportingContent = {
                val version = bundle.version?.takeIf { it.isNotBlank() }
                if (version == null && loadIssue == null) return@ListItem

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    version?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    loadIssue?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            trailingContent = {
                if (sourceEditMode) {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = bundle.uid != 0,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                } else {
                    IconButton(onClick = onExpandToggle, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(
                                if (expanded) R.string.collapse_content else R.string.expand_content
                            ),
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider()
    }
}