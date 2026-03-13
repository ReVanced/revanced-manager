package app.revanced.manager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.gigamole.composescrollbars.Scrollbars
import com.gigamole.composescrollbars.ScrollbarsState
import com.gigamole.composescrollbars.config.ScrollbarsConfig
import com.gigamole.composescrollbars.config.ScrollbarsOrientation
import com.gigamole.composescrollbars.config.layercontenttype.ScrollbarsLayerContentType
import com.gigamole.composescrollbars.config.layersType.ScrollbarsLayersType
import com.gigamole.composescrollbars.config.layersType.thicknessType.ScrollbarsThicknessType
import com.gigamole.composescrollbars.config.visibilitytype.ScrollbarsVisibilityType
import com.gigamole.composescrollbars.scrolltype.ScrollbarsScrollType
import com.gigamole.composescrollbars.scrolltype.knobtype.ScrollbarsStaticKnobType

@Composable
fun Scrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    Scrollbar(
        ScrollbarsScrollType.Scroll(
            knobType = ScrollbarsStaticKnobType.Auto(),
            state = scrollState
        ),
        modifier
    )
}

@Composable
fun Scrollbar(lazyListState: LazyListState, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val canScroll = lazyListState.canScrollForward || lazyListState.canScrollBackward

    val scrollInfo by remember {
        derivedStateOf {
            computeLazyScrollInfo(lazyListState.layoutInfo)
        }
    }

    val targetAlpha = if (lazyListState.isScrollInProgress && canScroll) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (targetAlpha > 0f)
            tween(durationMillis = 150)
        else
            tween(durationMillis = 500, delayMillis = 1000),
        label = "scrollbar_alpha"
    )

    if (alpha > 0f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val trackHeight = size.height
            if (trackHeight <= 0f) return@Canvas
            val thumbWidth = 4.dp.toPx()
            val minThumbHeight = 48.dp.toPx().coerceAtMost(trackHeight)

            val thumbHeight = (scrollInfo.viewportFraction * trackHeight)
                .coerceIn(minThumbHeight, trackHeight)
            val thumbTop = scrollInfo.scrollFraction * (trackHeight - thumbHeight)

            drawRoundRect(
                color = color.copy(alpha = 0.35f * alpha),
                topLeft = Offset(size.width - thumbWidth, thumbTop),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2)
            )
        }
    }
}

private data class LazyScrollInfo(
    val scrollFraction: Float,
    val viewportFraction: Float
)

private fun computeLazyScrollInfo(layoutInfo: LazyListLayoutInfo): LazyScrollInfo {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty() || layoutInfo.totalItemsCount == 0) return LazyScrollInfo(0f, 1f)

    val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    if (viewportSize <= 0) return LazyScrollInfo(0f, 1f)

    val totalItems = layoutInfo.totalItemsCount
    val firstItem = items.first()
    val lastItem = items.last()

    val hiddenAbove = if (items.size >= 2) {
        (items[1].index - firstItem.index - 1).coerceAtLeast(0)
    } else 0

    val effectiveFirstIndex = firstItem.index + hiddenAbove
    val effectiveVisibleCount = lastItem.index - effectiveFirstIndex + 1
    val scrollableRange = totalItems - effectiveVisibleCount

    if (scrollableRange <= 0) return LazyScrollInfo(0f, 1f)

    val subOffset = if (hiddenAbove > 0 && items.size >= 2) {
        val behindHeader = firstItem.size - items[1].offset
        (behindHeader.toFloat() / items[1].size.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else {
        (-firstItem.offset.toFloat() / firstItem.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    val scrollFraction = ((effectiveFirstIndex + subOffset) / scrollableRange).coerceIn(0f, 1f)
    val viewportFraction = (effectiveVisibleCount.toFloat() / totalItems).coerceIn(0.05f, 1f)

    return LazyScrollInfo(scrollFraction, viewportFraction)
}

@Composable
private fun Scrollbar(scrollType: ScrollbarsScrollType, modifier: Modifier = Modifier) {
    Scrollbars(
        state = ScrollbarsState(
            ScrollbarsConfig(
                orientation = ScrollbarsOrientation.Vertical,
                paddingValues = PaddingValues(0.dp),
                layersType = ScrollbarsLayersType.Wrap(ScrollbarsThicknessType.Exact(4.dp)),
                knobLayerContentType = ScrollbarsLayerContentType.Default.Colored.Idle(
                    idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                ),
                visibilityType = ScrollbarsVisibilityType.Dynamic.Fade(
                    isVisibleOnTouchDown = true,
                    isStaticWhenScrollPossible = false
                )
            ),
            scrollType
        ),
        modifier = modifier
    )
}