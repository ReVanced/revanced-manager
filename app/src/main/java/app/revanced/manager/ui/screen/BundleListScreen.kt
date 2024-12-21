package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.bundle.BundleItem

@Composable
fun BundleListScreen(
    onDelete: (PatchBundleSource) -> Unit,
    onUpdate: (PatchBundleSource) -> Unit,
    sources: List<PatchBundleSource>,
    selectedSources: SnapshotStateList<PatchBundleSource>,
    bundlesSelectable: Boolean,
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
                bundle = source,
                onDelete = {
                    onDelete(source)
                },
                onUpdate = {
                    onUpdate(source)
                },
                selectable = bundlesSelectable,
                onSelect = {
                    selectedSources.add(source)
                },
                isBundleSelected = selectedSources.contains(source),
                toggleSelection = { bundleIsNotSelected ->
                    if (bundleIsNotSelected) {
                        selectedSources.add(source)
                    } else {
                        selectedSources.remove(source)
                    }
                }
            )
        }
    }
}