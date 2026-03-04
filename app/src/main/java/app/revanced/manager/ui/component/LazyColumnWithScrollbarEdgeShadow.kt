package app.revanced.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LazyColumnWithScrollbarEdgeShadow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    edgeShadowHeight: Dp = 40.dp,
    edgeShadowProximity: Dp = 80.dp,
    content: LazyListScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val proximityPx = with(LocalDensity.current) { edgeShadowProximity.toPx() }

    val topAlpha by remember(state, proximityPx) {
        derivedStateOf {
            if (!state.canScrollBackward || proximityPx <= 0f)
                return@derivedStateOf 0f

            val layoutInfo = state.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf 0f

            if (firstVisible.index > 0)
                return@derivedStateOf 1f

            val viewportStart = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
            val overflowPx = (viewportStart - firstVisible.offset).coerceAtLeast(0)

            (overflowPx / proximityPx).coerceIn(0f, 1f)
        }
    }

    val bottomAlpha by remember(state, proximityPx) {
        derivedStateOf {
            if (!state.canScrollForward || proximityPx <= 0f)
                return@derivedStateOf 0f

            val layoutInfo = state.layoutInfo
            val lastIndex = layoutInfo.totalItemsCount - 1
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf 0f

            if (lastVisible.index < lastIndex)
                return@derivedStateOf 1f

            val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            val overflowPx = (lastVisible.offset + lastVisible.size - viewportEnd)
                .coerceAtLeast(0)

            (overflowPx / proximityPx).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.matchParentSize(),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )

        if (topAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(edgeShadowHeight)
                    .alpha(topAlpha)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(surfaceColor, Color.Transparent)
                        )
                    )
            )
        }

        if (bottomAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(edgeShadowHeight)
                    .alpha(bottomAlpha)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, surfaceColor)
                        )
                    )
            )
        }

        if (state.canScrollForward || state.canScrollBackward) {
            Scrollbar(state)
        }
    }
}