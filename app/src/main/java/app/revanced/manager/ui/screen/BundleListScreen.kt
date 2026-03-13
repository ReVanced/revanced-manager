package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.sources.Extensions.version
import app.revanced.manager.domain.sources.PatchBundleSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.SearchBar
import app.revanced.manager.ui.component.bundle.BundleInformationDialog
import app.revanced.manager.ui.component.bundle.BundlePatchList
import app.revanced.manager.ui.component.bundle.BundleSectionHeader
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.patches.OptionsDialog
import app.revanced.manager.ui.component.patches.bundlePatchListReadOnly
import app.revanced.manager.ui.viewmodel.BundleListViewModel
import app.revanced.manager.util.EventEffect
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleListScreen(
    viewModel: BundleListViewModel = koinViewModel(),
    eventsFlow: Flow<BundleListViewModel.Event>,
) {
    val patchCounts by viewModel.patchCounts.collectAsStateWithLifecycle(emptyMap())
    val sources by viewModel.sources.collectAsStateWithLifecycle(emptyList())
    val patchBundleRepository: PatchBundleRepository = koinInject()

    val allBundlePatches by patchBundleRepository.bundleInfoFlow
        .collectAsStateWithLifecycle(emptyMap())

    var optionsDialog by remember { mutableStateOf<PatchInfo?>(null) }
    val (query, setQuery) = rememberSaveable { mutableStateOf("") }
    val (searchExpanded, setSearchExpanded) = rememberSaveable { mutableStateOf(false) }

    optionsDialog?.let { patch ->
        OptionsDialog(
            patch = patch,
            onDismissRequest = { optionsDialog = null }
        )
    }

    EventEffect(eventsFlow) {
        viewModel.handleEvent(it)
    }

    Column {
        SearchBar(
            query = query,
            onQueryChange = setQuery,
            expanded = searchExpanded,
            onExpandedChange = setSearchExpanded,
            placeholder = { Text(stringResource(R.string.search)) },
            windowInsets = WindowInsets(0),
            leadingIcon = {
                if (searchExpanded) {
                    IconButton(onClick = { setSearchExpanded(false) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                } else {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }
            },
            trailingIcon = {
                if (searchExpanded && query.isNotEmpty()) {
                    IconButton(onClick = { setQuery("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                }
            }
        ) {
            BundlePatchList(
                bundles = sources,
                uid = { it.uid },
                modifier = Modifier.fillMaxSize(),
                autoExpandInitial = true,
                headerContent = { source, expanded, _ ->
                    if (sources.size > 1) {
                        BundleSectionHeader(
                            name = source.name,
                            version = source.version,
                            expanded = expanded,
                        )
                    }
                },
                patchContent = { source ->
                    val patches = allBundlePatches[source.uid]?.patches.orEmpty()
                        .filter { it.name.contains(query, true) }
                    bundlePatchListReadOnly(patches = patches)
                }
            )
        }

        PullToRefreshBox(
            onRefresh = viewModel::refresh,
            isRefreshing = viewModel.isRefreshing,
            modifier = Modifier.weight(1f)
        ) {
            BundlePatchList(
                bundles = sources,
                uid = { it.uid },
                modifier = Modifier.fillMaxSize(),
                headerContent = { source, expanded, onToggleExpand ->
                    BundleListHeader(
                        source = source,
                        patchCount = patchCounts[source.uid] ?: 0,
                        expanded = expanded,
                        onToggleExpand = onToggleExpand,
                        onDelete = { viewModel.delete(source) },
                        onUpdate = { viewModel.update(source) },
                        selectable = viewModel.selectedSources.isNotEmpty(),
                        isBundleSelected = source.uid in viewModel.selectedSources,
                        toggleSelection = { bundleIsNotSelected ->
                            if (bundleIsNotSelected) {
                                viewModel.selectedSources.add(source.uid)
                            } else {
                                viewModel.selectedSources.remove(source.uid)
                            }
                        }
                    )
                },
                patchContent = { source ->
                    val patches = allBundlePatches[source.uid]?.patches.orEmpty()
                    bundlePatchListReadOnly(
                        patches = patches,
                        onOptionsDialog = { patch -> optionsDialog = patch }
                    )
                }
            )
        }
    }
}

@Composable
fun BundleListHeader(
    source: PatchBundleSource,
    patchCount: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    selectable: Boolean,
    isBundleSelected: Boolean,
    toggleSelection: (Boolean) -> Unit,
) {
    var viewBundleDialogPage by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (viewBundleDialogPage) {
        BundleInformationDialog(
            src = source,
            onDismissRequest = { viewBundleDialogPage = false },
            onDeleteRequest = { showDeleteConfirmationDialog = true },
            onUpdate = onUpdate,
        )
    }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                onDelete()
                viewBundleDialogPage = false
            },
            title = stringResource(R.string.delete),
            description = stringResource(R.string.patches_delete_single_dialog_description, source.name),
            icon = Icons.Outlined.Delete
        )
    }

    BundleSectionHeader(
        name = source.name,
        version = source.version,
        expanded = expanded,
        onToggleExpand = onToggleExpand,
        onClick = { viewBundleDialogPage = true },
        patchCount = patchCount.takeIf { source.state is Source.State.Available<*> },
        trailingContent = if (selectable) {
            {
                HapticCheckbox(
                    checked = isBundleSelected,
                    onCheckedChange = toggleSelection,
                )
            }
        } else {
            val icon = remember(source.state) {
                when (source.state) {
                    is Source.State.Failed -> Icons.Outlined.ErrorOutline to R.string.patches_error
                    is Source.State.Missing -> Icons.Outlined.Warning to R.string.patches_missing
                    is Source.State.Available<*> -> null
                }
            }
            icon?.let { (vector, description) ->
                {
                    Icon(
                        vector,
                        contentDescription = stringResource(description),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
    )
}