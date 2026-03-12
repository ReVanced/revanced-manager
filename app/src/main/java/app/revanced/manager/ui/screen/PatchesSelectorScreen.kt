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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.CheckedFilterChip
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SafeguardDialog
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTab
import app.revanced.manager.ui.component.haptics.HapticTriStateCheckbox
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.component.patches.SelectionWarningDialog
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_INCOMPATIBLE
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

enum class SourceMenuAction { MORE, REFRESH }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun PatchesSelectorScreen(
    onSave: (PatchSelection?, Options) -> Unit,
    onBackClick: () -> Unit,
    viewModel: PatchesSelectorViewModel,
    sourceEditMode: Boolean = false,
    selectedSourceUids: Set<Int> = emptySet(),
    onToggleSourceSelection: ((Int) -> Unit)? = null,
    onCurrentSourceChanged: ((Int?) -> Unit)? = null,
    onSourceMenuAction: ((uid: Int, action: SourceMenuAction) -> Unit)? = null,
) {
    val context = LocalContext.current
    val readOnly = viewModel.readOnly
    val bundles by viewModel.bundlesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val sourceStates by viewModel.sourceStateByUidFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
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
    var selectedReadonlyPackages by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val showSaveButton by remember {
        derivedStateOf { viewModel.selectionIsValid(bundles) }
    }

    val defaultPatchSelectionCount by viewModel.defaultSelectionCount
        .collectAsStateWithLifecycle(initialValue = 0)

    val selectedPatchCount by remember {
        derivedStateOf {
            viewModel.customPatchSelection?.values?.sumOf { it.size } ?: defaultPatchSelectionCount
        }
    }

    val patchLazyListStates = remember(bundles) { List(bundles.size) { LazyListState() } }

    var showSelectionWarning by rememberSaveable { mutableStateOf(false) }
    var showUniversalWarning by rememberSaveable { mutableStateOf(false) }

    var pendingScopeAction by remember { mutableStateOf<((Int?) -> Unit)?>(null) }

    fun executeScopedAction(action: (Int?) -> Unit) {
        if (bundles.size > 1) {
            pendingScopeAction = action
        } else {
            action(bundles.firstOrNull()?.uid)
        }
    }

    fun PatchInfo.matchesReadonlyPackageFilter(): Boolean {
        if (!readOnly || selectedReadonlyPackages.isEmpty()) return true

        // Universal patches apply to any package and should remain visible.
        val compatiblePackageList = compatiblePackages ?: return true
        return compatiblePackageList.any { it.packageName in selectedReadonlyPackages }
    }

    fun List<PatchInfo>.applyReadonlyPackageFilter() =
        if (!readOnly) this else filter(PatchInfo::matchesReadonlyPackageFilter)

    LaunchedEffect(bundles, pagerState.currentPage) {
        onCurrentSourceChanged?.invoke(bundles.getOrNull(pagerState.currentPage)?.uid)
    }

    fun LazyListScope.sourceStateItem(uid: Int) {
        when (val state = sourceStates[uid]) {
            is PatchBundleSource.State.Failed -> {
                item(key = "source-error-$uid") {
                    ListHeader(title = stringResource(R.string.patches_error))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.patches_error_description)) },
                        supportingContent = { Text(state.throwable.message.orEmpty()) },
                        colors = transparentListItemColors
                    )
                }
            }

            PatchBundleSource.State.Missing -> {
                item(key = "source-missing-$uid") {
                    ListHeader(title = stringResource(R.string.patches_missing))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.patches_not_downloaded)) },
                        colors = transparentListItemColors
                    )
                }
            }

            else -> Unit
        }
    }

    fun LazyListScope.emptyPatchesItem(uid: Int) {
        item(key = "source-empty-$uid") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.no_patches_selected)) },
                colors = transparentListItemColors
            )
        }
    }

    pendingScopeAction?.let { action ->
        val currentBundle = bundles.getOrNull(pagerState.currentPage) ?: return@let

        ScopeDialog(
            bundleName = currentBundle.name,
            onDismissRequest = { pendingScopeAction = null },
            onAllPatches = {
                action(null)
                pendingScopeAction = null
            },
            onBundleOnly = {
                action(currentBundle.uid)
                pendingScopeAction = null
            }
        )
    }

    if (showBottomSheet) {
        val currentBundle = bundles.getOrNull(pagerState.currentPage)

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

                data class PackageFilterOption(
                    val packageName: String,
                    val displayName: String,
                    val patchCount: Int,
                )

                val packageOptions = remember(currentBundle?.uid, bundles) {
                    val pm = context.packageManager

                    // Count how many patches explicitly target each package.
                    val packagePatchCounts = mutableMapOf<String, Int>()
                    currentBundle
                        ?.patches
                        .orEmpty()
                        .forEach { patch ->
                            patch.compatiblePackages
                                ?.map { it.packageName }
                                ?.distinct()
                                ?.forEach { packageName ->
                                    packagePatchCounts[packageName] = (packagePatchCounts[packageName] ?: 0) + 1
                                }
                        }

                    packagePatchCounts.map { (packageName, count) ->
                        val appInfo = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
                        val appLabel = appInfo
                            ?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
                            .orEmpty()
                            .ifBlank { packageName }

                        PackageFilterOption(
                            packageName = packageName,
                            displayName = appLabel,
                            patchCount = count,
                        )
                    }.sortedWith(
                        compareByDescending<PackageFilterOption> { it.patchCount }
                            .thenBy { it.displayName.lowercase() }
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!readOnly) {
                        CheckedFilterChip(
                            selected = viewModel.filter and SHOW_INCOMPATIBLE == 0,
                            onClick = { viewModel.toggleFlag(SHOW_INCOMPATIBLE) },
                            label = { Text(stringResource(R.string.this_version)) }
                        )
                    }

                    CheckedFilterChip(
                        selected = viewModel.filter and SHOW_UNIVERSAL != 0,
                        onClick = { viewModel.toggleFlag(SHOW_UNIVERSAL) },
                        label = { Text(stringResource(R.string.universal)) },
                    )

                    if (readOnly) {
                        packageOptions.forEach { packageOption ->
                             CheckedFilterChip(
                                selected = packageOption.packageName in selectedReadonlyPackages,
                                onClick = {
                                    selectedReadonlyPackages = if (packageOption.packageName in selectedReadonlyPackages) {
                                        selectedReadonlyPackages - packageOption.packageName
                                    } else {
                                        selectedReadonlyPackages + packageOption.packageName
                                    }
                                },
                                label = {
                                    Text("${packageOption.displayName} (${packageOption.patchCount})")
                                }
                             )
                         }
                    }
                }

                if (!readOnly) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = stringResource(R.string.patch_selector_sheet_actions_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    fun guardedAction(action: () -> Unit) {
                        showBottomSheet = false
                        if (viewModel.selectionWarningEnabled) {
                            showSelectionWarning = true
                        } else {
                            action()
                        }
                    }

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

                    if (bundles.size > 1 && currentBundle != null) {
                        ActionItem(
                            icon = Icons.Outlined.Deselect,
                            text = stringResource(R.string.deselect_all_except, currentBundle.name),
                            onClick = {
                                guardedAction {
                                    viewModel.deselectAllExcept(bundles, currentBundle.uid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (viewModel.compatibleVersions.isNotEmpty())
        IncompatiblePatchDialog(
            appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
            compatibleVersions = viewModel.compatibleVersions,
            onDismissRequest = viewModel::dismissDialogs
        )
    var showIncompatiblePatchesDialog by rememberSaveable {
        mutableStateOf(false)
    }
    if (showIncompatiblePatchesDialog)
        IncompatiblePatchesDialog(
            appVersion = viewModel.appVersion ?: stringResource(R.string.any_version),
            onDismissRequest = { showIncompatiblePatchesDialog = false }
        )

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

    if (showSelectionWarning)
        SelectionWarningDialog(onDismiss = { showSelectionWarning = false })

    if (showUniversalWarning)
        UniversalPatchWarningDialog(onDismiss = { showUniversalWarning = false })

    fun LazyListScope.patchList(
        uid: Int,
        patches: List<PatchInfo>,
        visible: Boolean,
        compatible: Boolean,
        section: String,
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
                key = { "$section:${it.name}" },
                contentType = { 1 }
            ) { patch ->
                PatchItem(
                    patch = patch,
                    onOptionsDialog = { viewModel.optionsDialog = uid to patch },
                    selected = compatible && viewModel.isSelected(uid, patch),
                    onToggle = if (readOnly) ({}) else ({
                        when {
                            // Open incompatible dialog if the patch is not supported
                            !compatible -> viewModel.openIncompatibleDialog(patch)

                            // Show selection warning if enabled
                            viewModel.selectionWarningEnabled -> showSelectionWarning = true

                            // Show universal warning if universal patch is selected and the toggle is off
                            patch.compatiblePackages == null && viewModel.universalPatchWarningEnabled -> showUniversalWarning =
                                true

                            // Toggle the patch otherwise
                            else -> viewModel.togglePatch(uid, patch)
                        }
                    }),
                    compatible = compatible,
                    readOnly = readOnly,
                    packageName = viewModel.packageName,
                    showAllPackages = readOnly
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
                val bundle = bundles.getOrNull(pagerState.currentPage)
                if (bundle == null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.patches_error)) },
                        supportingContent = { Text(stringResource(R.string.patches_error_description)) },
                        colors = transparentListItemColors
                    )
                    return@SearchBar
                }

                LazyColumnWithScrollbar(
                    modifier = Modifier.fillMaxSize()
                ) {
                    sourceStateItem(bundle.uid)
                    fun List<PatchInfo>.searched() =
                        applyReadonlyPackageFilter().filter { it.name.contains(query, true) }

                    val compatiblePatches = bundle.compatible.searched()
                    val universalPatches = bundle.universal.searched()
                    val incompatiblePatches = bundle.incompatible.searched()

                    patchList(
                        uid = bundle.uid,
                        patches = compatiblePatches,
                        visible = true,
                        compatible = true,
                        section = "compatible"
                    )
                    patchList(
                        uid = bundle.uid,
                        patches = universalPatches,
                        visible = viewModel.filter and SHOW_UNIVERSAL != 0,
                        compatible = true,
                        section = "universal"
                    ) {
                        ListHeader(
                            title = stringResource(R.string.universal_patches),
                        )
                    }

                    patchList(
                        uid = bundle.uid,
                        patches = incompatiblePatches,
                        visible = viewModel.filter and SHOW_INCOMPATIBLE != 0,
                        compatible = viewModel.allowIncompatiblePatches,
                        section = "incompatible"
                    ) {
                        ListHeader(
                            title = stringResource(R.string.incompatible_patches),
                            onHelpClick = { showIncompatiblePatchesDialog = true }
                        )
                    }

                    if (compatiblePatches.isEmpty() && universalPatches.isEmpty() && incompatiblePatches.isEmpty()) {
                        emptyPatchesItem(bundle.uid)
                    }
                }
            }
        },
        floatingActionButton = {
            if (readOnly || !showSaveButton) return@Scaffold

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

                    val isScrollingUp =
                        patchLazyListStates.getOrNull(pagerState.currentPage)?.isScrollingUp()
                    val expanded by produceState(true, isScrollingUp) {
                        val state = isScrollingUp ?: return@produceState
                        value = state.value

                        // Use snapshotFlow and sample to prevent the value from changing too often.
                        snapshotFlow { state.value }
                            .sample(333L)
                            .collect {
                                value = it
                            }
                    }

                    HapticExtendedFloatingActionButton(
                        text = {
                            Text(
                                stringResource(
                                    R.string.save_with_count,
                                    selectedPatchCount
                                )
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 16.dp)
        ) {
            if (bundles.size > 1) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
                ) {
                    bundles.forEachIndexed { index, bundle ->
                        var showTabMenu by remember { mutableStateOf(false) }

                        HapticTab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                if (sourceEditMode && readOnly) {
                                    onToggleSourceSelection?.invoke(bundle.uid)
                                } else {
                                    composableScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (sourceEditMode && readOnly) {
                                        HapticCheckbox(
                                            checked = bundle.uid in selectedSourceUids,
                                            onCheckedChange = { onToggleSourceSelection?.invoke(bundle.uid) }
                                        )
                                    } else {
                                        val selectionState = viewModel.getBundleSelectionState(bundle)
                                        val toggleableState = when (selectionState) {
                                            true -> ToggleableState.On
                                            false -> ToggleableState.Off
                                            null -> ToggleableState.Indeterminate
                                        }
                                        HapticTriStateCheckbox(
                                            state = toggleableState,
                                            onClick = if (readOnly) ({}) else ({
                                                when {
                                                    viewModel.selectionWarningEnabled -> showSelectionWarning = true
                                                    selectionState == false -> viewModel.restoreDefaults(bundle.uid)
                                                    else -> viewModel.deselectAll(bundles, bundle.uid)
                                                }
                                            }),
                                            enabled = !readOnly
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = bundle.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = bundle.version.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (onSourceMenuAction != null) {
                                        Box {
                                            IconButton(
                                                onClick = { showTabMenu = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.MoreVert,
                                                    contentDescription = stringResource(R.string.more),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showTabMenu,
                                                onDismissRequest = { showTabMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.refresh)) },
                                                    leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                                    onClick = {
                                                        showTabMenu = false
                                                        onSourceMenuAction(bundle.uid, SourceMenuAction.REFRESH)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.more)) },
                                                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                                                    onClick = {
                                                        showTabMenu = false
                                                        onSourceMenuAction(bundle.uid, SourceMenuAction.MORE)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
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
                        sourceStateItem(bundle.uid)

                        val compatiblePatches = bundle.compatible.applyReadonlyPackageFilter()
                        val universalPatches = bundle.universal.applyReadonlyPackageFilter()
                        val incompatiblePatches = bundle.incompatible.applyReadonlyPackageFilter()

                        patchList(
                            uid = bundle.uid,
                            patches = compatiblePatches,
                            visible = true,
                            compatible = true,
                            section = "compatible"
                        )
                        patchList(
                            uid = bundle.uid,
                            patches = universalPatches,
                            visible = viewModel.filter and SHOW_UNIVERSAL != 0,
                            compatible = true,
                            section = "universal"
                        ) {
                            ListHeader(
                                title = stringResource(R.string.universal_patches),
                            )
                        }
                        patchList(
                            uid = bundle.uid,
                            patches = incompatiblePatches,
                            visible = viewModel.filter and SHOW_INCOMPATIBLE != 0,
                            compatible = viewModel.allowIncompatiblePatches,
                            section = "incompatible"
                        ) {
                            ListHeader(
                                title = stringResource(R.string.incompatible_patches),
                                onHelpClick = { showIncompatiblePatchesDialog = true }
                            )
                        }

                        if (compatiblePatches.isEmpty() && universalPatches.isEmpty() && incompatiblePatches.isEmpty()) {
                            emptyPatchesItem(bundle.uid)
                        }
                    }
                }
            )
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

@Composable
private fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: (() -> Unit)?,
    selected: Boolean,
    onToggle: () -> Unit,
    compatible: Boolean = true,
    readOnly: Boolean = false,
    packageName: String = "",
    showAllPackages: Boolean = false
) = ListItem(
    modifier = Modifier
        .let { if (!compatible) it.alpha(0.5f) else it }
        .let { if (!readOnly) it.clickable(onClick = onToggle) else it }
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
        Column {
            patch.description?.let {
                Text(it)
            }
            // Show compatibility information
            PatchCompatibilityInfo(
                patch = patch,
                packageName = packageName,
                showAllPackages = showAllPackages
            )
        }
    },
    trailingContent = {
        if (patch.options?.isNotEmpty() == true && onOptionsDialog != null) {
            IconButton(onClick = onOptionsDialog, enabled = compatible) {
                Icon(Icons.Outlined.Settings, null)
            }
        }
    },
    colors = transparentListItemColors
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatchCompatibilityInfo(
    patch: PatchInfo,
    packageName: String = "",
    showAllPackages: Boolean = false
) {
    if (patch.compatiblePackages == null) {
        // Universal patch
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            CompatibilityChip(text = "📦 ${stringResource(R.string.patches_view_any_package)}")
            CompatibilityChip(text = "🎯 ${stringResource(R.string.patches_view_any_version)}")
        }
    } else {
        // Specific package compatibility
        if (showAllPackages) {
            // Show all packages (readonly mode)
            patch.compatiblePackages.forEach { compatiblePackage ->
                val versions = compatiblePackage.versions.orEmpty().reversed()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    CompatibilityChip(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = "📦 ${compatiblePackage.packageName}"
                    )
                    if (versions.isEmpty()) {
                        CompatibilityChip(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "🎯 ${stringResource(R.string.patches_view_any_version)}"
                        )
                    } else {
                        versions.forEach { version ->
                            CompatibilityChip(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                text = "🎯 $version"
                            )
                        }
                    }
                }
            }
        } else {
            // Show only selected app's compatible versions (patching mode)
            val selectedPackageCompat = patch.compatiblePackages.find { it.packageName == packageName }
            if (selectedPackageCompat != null) {
                val versions = selectedPackageCompat.versions.orEmpty().reversed()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (versions.isEmpty()) {
                        CompatibilityChip(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "🎯 ${stringResource(R.string.patches_view_any_version)}"
                        )
                    } else {
                        versions.forEach { version ->
                            CompatibilityChip(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                text = "🎯 $version"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatibilityChip(
    modifier: Modifier = Modifier,
    text: String
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
private fun IncompatiblePatchesDialog(
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
        TextButton(onClick = onDismissRequest) {
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

@Composable
private fun ActionItem(
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
        TextButton(onClick = onAllPatches) {
            Text(stringResource(R.string.scope_all_patches))
        }
    },
    dismissButton = {
        TextButton(onClick = onBundleOnly) {
            Text(stringResource(R.string.scope_bundle_patches, bundleName))
        }
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
                        IconButton(onClick = reset) {
                            Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
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
                    setValue = if (readOnly) ({}) else ({ set(name, it) }),
                    selectionWarningEnabled = selectionWarningEnabled,
                    readOnly = readOnly
                )
            }
        }
    }
}
