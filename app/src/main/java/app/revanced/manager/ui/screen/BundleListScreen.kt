package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.bundle.BundleItem
import app.revanced.manager.ui.viewmodel.BundleListViewModel
import app.revanced.manager.util.EventEffect
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleListScreen(
    viewModel: BundleListViewModel = koinViewModel(),
    eventsFlow: Flow<BundleListViewModel.Event>,
    setSelectedSourceCount: (Int) -> Unit
) {
    val patchCounts by viewModel.patchCounts.collectAsStateWithLifecycle(emptyMap())
    val sources by viewModel.sources.collectAsStateWithLifecycle(emptyList())

    EventEffect(eventsFlow) {
        viewModel.handleEvent(it)
    }
    LaunchedEffect(viewModel.selectedSources.size) {
        setSelectedSourceCount(viewModel.selectedSources.size)
    }

    PullToRefreshBox(
        onRefresh = viewModel::refresh,
        isRefreshing = viewModel.isRefreshing
    ) {
        LazyColumnWithScrollbar(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            items(
                sources,
                key = { it.uid }
            ) { source ->
                BundleItem(
                    src = source,
                    patchCount = patchCounts[source.uid] ?: 0,
                    onDelete = {
                        viewModel.delete(source)
                    },
                    onUpdate = {
                        viewModel.update(source)
                    },
                    selectable = viewModel.selectedSources.size > 0,
                    onSelect = {
                        viewModel.selectedSources.add(source.uid)
                    },
                    isBundleSelected = source.uid in viewModel.selectedSources,
                    toggleSelection = { bundleIsNotSelected ->
                        if (bundleIsNotSelected) {
                            viewModel.selectedSources.add(source.uid)
                        } else {
                            viewModel.selectedSources.remove(source.uid)
                        }
                    }
                )
            }
        }
    }
}