package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticTab
import app.revanced.manager.ui.component.patches.OptionItem
import app.revanced.manager.ui.model.BundleInfo.Extensions.requiredOptionsSet
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.isScrollingUp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredOptionsScreen(
    onContinue: (PatchSelection?, Options) -> Unit,
    onBackClick: () -> Unit,
    vm: PatchesSelectorViewModel
) {
    val list by vm.requiredOptsPatches.collectAsStateWithLifecycle(emptyList())

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        list.size
    }
    val patchLazyListStates = remember(list) { List(list.size, ::LazyListState) }
    val bundles by vm.bundlesFlow.collectAsStateWithLifecycle(emptyList())
    val showContinueButton by remember {
        derivedStateOf {
            bundles.requiredOptionsSet(
                isSelected = { bundle, patch -> vm.isSelected(bundle.uid, patch) },
                optionsForPatch = { bundle, patch -> vm.getOptions(bundle.uid, patch) }
            )
        }
    }
    val composableScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.required_options_screen),
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            if (!showContinueButton) return@Scaffold

            HapticExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.patch)) },
                icon = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        stringResource(R.string.patch)
                    )
                },
                expanded = patchLazyListStates.getOrNull(pagerState.currentPage)?.isScrollingUp
                    ?: true,
                onClick = {
                    onContinue(vm.getCustomSelection(), vm.getOptions())
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (list.isEmpty()) return@Column
            else if (list.size > 1) ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)
            ) {
                list.forEachIndexed { index, (bundle, _) ->
                    HapticTab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            composableScope.launch {
                                pagerState.animateScrollToPage(
                                    index
                                )
                            }
                        },
                        text = { Text(bundle.name) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                pageContent = { index ->
                    // Avoid crashing if the lists have not been fully initialized yet.
                    if (index > list.lastIndex || list.size != patchLazyListStates.size) return@HorizontalPager
                    val (bundle, patches) = list[index]

                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        state = patchLazyListStates[index]
                    ) {
                        items(patches, key = { it.name }) {
                            ListHeader(it.name)

                            val values = vm.getOptions(bundle.uid, it)
                            it.options?.forEach { option ->
                                val key = option.key
                                val value =
                                    if (values == null || key !in values) option.default else values[key]

                                @Suppress("UNCHECKED_CAST")
                                OptionItem(
                                    option = option as Option<Any>,
                                    value = value,
                                    setValue = { new ->
                                        vm.setOption(bundle.uid, it, key, new)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}