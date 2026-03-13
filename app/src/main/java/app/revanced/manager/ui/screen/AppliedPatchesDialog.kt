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
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstalledPatchBundle
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.patches.ListHeader
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppliedPatchesDialog(
    onDismissRequest: () -> Unit,
    appliedPatches: PatchSelection?,
    patchBundles: List<InstalledPatchBundle>
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    FullscreenDialog(onDismissRequest = onDismissRequest) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.applied_patches),
                    scrollBehavior = scrollBehavior,
                    onBackClick = onDismissRequest
                )
            },
        ) { paddingValues ->
            LazyColumnWithScrollbar(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val patches = appliedPatches ?: return@LazyColumnWithScrollbar
                val bundleMap = patchBundles.associateBy { it.bundleUid }

                patches.forEach { (bundleUid, patchNames) ->
                    val bundle = bundleMap[bundleUid]

                    item(key = "header_$bundleUid") {
                        ListHeader(title = bundle?.let {
                            it.bundleVersion?.let { version ->
                                "${it.bundleName} v$version"
                            } ?: it.bundleName
                        } ?: "${stringResource(R.string.patches)} $bundleUid")
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
}