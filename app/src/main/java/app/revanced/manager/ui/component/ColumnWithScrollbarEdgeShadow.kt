package app.revanced.manager.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ColumnWithScrollbarEdgeShadow(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    edgeShadowHeight: Dp = 40.dp,
    edgeShadowProximity: Dp = 80.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val proximityPx = with(LocalDensity.current) { edgeShadowProximity.toPx() }

    val bottomAlpha by remember(state, proximityPx) {
        derivedStateOf {
            val maxScroll = state.maxValue.takeUnless { it == Int.MAX_VALUE } ?: 0
            if (maxScroll == 0 || proximityPx <= 0f) {
                0f
            } else {
                ((maxScroll - state.value).coerceAtLeast(0) / proximityPx).coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .matchParentSize()
                .verticalScroll(state),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(edgeShadowHeight)
                .graphicsLayer { alpha = bottomAlpha }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, surfaceColor)
                    )
                )
        )
        if (state.canScrollForward || state.canScrollBackward) {
            Scrollbar(state)
        }
    }
}