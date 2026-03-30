package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.ui.component.CheckedFilterChip
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_INCOMPATIBLE
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL
import app.revanced.manager.util.PM
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchesFilterBottomSheet(
    onDismissRequest: () -> Unit,
    sections: List<BundleSection>,
    patchLazyListState: LazyListState,
    bundles: List<PatchBundleInfo.Scoped>,
    filter: Int,
    onToggleFlag: (Int) -> Unit,
    packageName: String? = null,
    readOnly: Boolean,
    selectionWarningEnabled: Boolean,
    onShowSelectionWarning: () -> Unit,
    onRestoreDefaults: (Int?) -> Unit,
    onDeselectAll: (Int?) -> Unit,
    onInvertSelection: (Int?) -> Unit,
    onDeselectAllExcept: (List<PatchBundleInfo.Scoped>, Int) -> Unit,
    selectedPackageFilters: Set<String> = emptySet(),
    onTogglePackageFilter: (String) -> Unit = {},
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val pm: PM = koinInject()
    val pmAppList by pm.appList.collectAsStateWithLifecycle(initialValue = emptyList())

    var pendingScopeAction by remember { mutableStateOf<((Int?) -> Unit)?>(null) }

    val sectionLayouts = remember(sections) { buildSectionLayouts(sections) }
    val currentBundle by remember(sectionLayouts, patchLazyListState) {
        derivedStateOf {
            sectionLayouts.lastOrNull { it.headerIndex <= patchLazyListState.firstVisibleItemIndex }?.bundle
                ?: bundles.firstOrNull()
        }
    }

    val packagePatchCounts = remember(bundles) {
        bundles.asSequence()
            .flatMap { it.patches.asSequence() }
            .flatMap { it.compatiblePackages.orEmpty().asSequence() }
            .groupingBy { it.packageName }
            .eachCount()
    }

    val installedPackageLabels = remember(pmAppList) {
        pmAppList.filter { it.packageInfo != null }
            .associate { appInfo ->
                appInfo.packageName to pm.run { appInfo.packageInfo!!.label() }
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    packageName?.let {
                        CheckedFilterChip(
                            selected = filter and SHOW_INCOMPATIBLE == 0,
                            onClick = { onToggleFlag(SHOW_INCOMPATIBLE) },
                            label = { Text(stringResource(R.string.this_version)) }
                        )
                    }

                    CheckedFilterChip(
                        selected = filter and SHOW_UNIVERSAL != 0,
                        onClick = { onToggleFlag(SHOW_UNIVERSAL) },
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        packagePatchCounts
                            .toList()
                            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
                            .forEach { (pkg, count) ->
                                val label = installedPackageLabels[pkg] ?: pkg
                                CheckedFilterChip(
                                    selected = pkg in selectedPackageFilters,
                                    onClick = { onTogglePackageFilter(pkg) },
                                    label = { Text("$label ($count)") }
                                )
                            }
                    }
                }
            }

            fun guardedAction(action: () -> Unit) {
                onDismissRequest()
                if (selectionWarningEnabled) {
                    onShowSelectionWarning()
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
                                    onRestoreDefaults(uid)
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
                                    onDeselectAll(uid)
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
                                    onInvertSelection(uid)
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
                                    onDeselectAllExcept(bundles, bundle.uid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}