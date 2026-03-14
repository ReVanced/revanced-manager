package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.EmptyState
import app.revanced.manager.ui.component.SearchBar
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
    setSelectedSourceCount: (Int) -> Unit,
    onBundleClick: (Int) -> Unit
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

    LaunchedEffect(viewModel.selectedSources.size) {
        setSelectedSourceCount(viewModel.selectedSources.size)
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
            if (sources.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Outlined.Source,
                        title = R.string.no_patches_found,
                        description = R.string.no_patches_description
                    )
                }
            } else {
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
                            onClick = { onBundleClick(source.uid) },
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
}

@Composable
private fun BundleListHeader(
    source: PatchBundleSource,
    patchCount: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    selectable: Boolean,
    isBundleSelected: Boolean,
    toggleSelection: (Boolean) -> Unit,
) {
    BundleSectionHeader(
        name = source.name,
        version = source.version,
        expanded = expanded,
        onToggleExpand = onToggleExpand,
        onClick = onClick,
        patchCount = patchCount.takeIf { source.state is PatchBundleSource.State.Available },
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
                    is PatchBundleSource.State.Failed -> Icons.Outlined.ErrorOutline to R.string.patches_error
                    is PatchBundleSource.State.Missing -> Icons.Outlined.Warning to R.string.patches_missing
                    is PatchBundleSource.State.Available -> null
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