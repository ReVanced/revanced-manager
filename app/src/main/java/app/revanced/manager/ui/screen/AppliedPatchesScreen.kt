package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.viewmodel.AppliedPatchesViewModel
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppliedPatchesScreen(
    onBackClick: () -> Unit,
    viewModel: AppliedPatchesViewModel
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.applied_patches),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val appliedPatches = viewModel.appliedPatches ?: return@LazyColumnWithScrollbar
            val bundleMap = viewModel.patchBundles.associateBy { it.bundleUid }

            appliedPatches.forEach { (bundleUid, patchNames) ->
                val bundle = bundleMap[bundleUid]
                val headerText = bundle?.let {
                    it.bundleVersion?.let { version ->
                        "${it.bundleName} v$version"
                    } ?: it.bundleName
                } ?: "Bundle $bundleUid"

                item(key = "header_$bundleUid") {
                    ListHeader(title = headerText)
                }

                items(
                    items = patchNames.sorted(),
                    key = { "${bundleUid}_$it" }
                ) { patchName ->
                    ListItem(
                        headlineContent = { Text(patchName) },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    }
}
