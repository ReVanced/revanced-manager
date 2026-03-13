package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.util.mutableStateSetOf

@Composable
fun <T> BundlePatchList(
    bundles: List<T>,
    uid: (T) -> Int,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    autoExpandInitial: Boolean = false,
    headerContent: @Composable (bundle: T, expanded: Boolean, onToggleExpand: () -> Unit) -> Unit,
    patchContent: LazyListScope.(bundle: T) -> Unit
) {
    val expandedBundles = remember { mutableStateSetOf<Int>() }

    if (autoExpandInitial) {
        val initializedBundles = remember { mutableSetOf<Int>() }
        bundles.forEach { bundle ->
            val id = uid(bundle)
            if (id !in initializedBundles) {
                initializedBundles.add(id)
                expandedBundles.add(id)
            }
        }
    }

    LazyColumnWithScrollbar(
        modifier = modifier,
        state = lazyListState
    ) {
        bundles.forEach { bundle ->
            val id = uid(bundle)
            val isExpanded = id in expandedBundles

            stickyHeader(key = "header_$id") {
                val isPinned by remember {
                    derivedStateOf {
                        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.key == "header_$id"
                                && lazyListState.canScrollBackward
                    }
                }
                val elevation by animateDpAsState(
                    targetValue = if (isPinned) 3.dp else 0.dp,
                    label = "header elevation"
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = elevation
                ) {
                    headerContent(bundle, isExpanded) {
                        if (isExpanded) expandedBundles.remove(id)
                        else expandedBundles.add(id)
                    }
                }
            }

            if (isExpanded) {
                patchContent(bundle)
            }
        }
    }
}

@Composable
fun BundleSectionHeader(
    name: String,
    version: String?,
    expanded: Boolean,
    onToggleExpand: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    patchCount: Int? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val expandRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "expand rotation"
    )

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                val clickAction = onClick ?: onToggleExpand
                if (clickAction != null) mod.clickable(onClick = clickAction) else mod
            },
        headlineContent = {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            val parts = buildList {
                version?.let { add(it) }
                patchCount?.let { add("$it patches") }
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = if (trailingContent != null || onToggleExpand != null) {
            {
                trailingContent?.invoke()
                onToggleExpand?.let { toggle ->
                    IconButton(onClick = toggle) {
                        Icon(
                            modifier = Modifier.rotate(expandRotation),
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.collapse_content
                                else R.string.expand_content
                            )
                        )
                    }
                }
            }
        } else null
    )
}
