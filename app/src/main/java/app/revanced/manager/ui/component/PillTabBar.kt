package app.revanced.manager.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PillTabBar(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    colors: PillTabBarColors = PillTabBarDefaults.colors(),
    tabs: @Composable RowScope.() -> Unit
) {
    val tabCount = pagerState.pageCount.coerceAtLeast(1)
    val state = rememberPillTabBarState(pagerState, tabCount)
    val indicatorScale by animatePillTabScale(state.pressedTabIndex == pagerState.currentPage)

    BoxWithConstraints(
        modifier = modifier
            .height(PillTabBarDefaults.ContainerHeight)
            .clip(CircleShape)
            .background(colors.containerColor)
            .padding(PillTabBarDefaults.ContainerPadding)
    ) {
        val indicatorWidthPx = with(LocalDensity.current) { maxWidth.toPx() } / tabCount
        val offsetX = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * indicatorWidthPx

        PillTabIndicator(
            offsetX = offsetX,
            tabCount = tabCount,
            currentPage = pagerState.currentPage,
            scale = indicatorScale,
            color = colors.indicatorColor
        )

        CompositionLocalProvider(
            LocalPillTabBarState provides state,
            LocalPillTabBarColors provides colors
        ) {
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = tabs
            )
        }
    }
}

@Composable
fun RowScope.PillTab(
    index: Int,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val view = LocalView.current
    val state = LocalPillTabBarState.current
    val colors = LocalPillTabBarColors.current
    val isSelected = state.pagerState.currentPage == index
    val contentScale by animatePillTabScale(state.pressedTabIndex == index)

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
                transformOrigin = transformOriginForIndex(index, state.tabCount)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        state.onPressedTabIndexChange(index)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        try {
                            awaitRelease()
                        } finally {
                            state.onPressedTabIndexChange(-1)
                        }
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        PillTabContent(
            isSelected = isSelected,
            colors = colors,
            icon = icon,
            text = text
        )
    }
}

@Composable
private fun PillTabIndicator(
    offsetX: Float,
    tabCount: Int,
    currentPage: Int,
    scale: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(1f / tabCount)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = transformOriginForIndex(currentPage, tabCount)
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PillTabContent(
    isSelected: Boolean,
    colors: PillTabBarColors,
    icon: (@Composable () -> Unit)?,
    text: @Composable () -> Unit
) {
    val contentColor = if (isSelected) colors.selectedContentColor else colors.unselectedContentColor
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = fontWeight)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PillTabBarDefaults.IconTextSpacing)
            ) {
                icon?.invoke()
                text()
            }
        }
    }
}

@Composable
private fun rememberPillTabBarState(
    pagerState: PagerState,
    tabCount: Int
): PillTabBarState {
    var pressedTabIndex by remember { mutableIntStateOf(-1) }
    return remember(pagerState, tabCount, pressedTabIndex) {
        PillTabBarState(
            pagerState = pagerState,
            tabCount = tabCount,
            pressedTabIndex = pressedTabIndex,
            onPressedTabIndexChange = { pressedTabIndex = it }
        )
    }
}

@Composable
private fun animatePillTabScale(isPressed: Boolean) = animateFloatAsState(
    targetValue = if (isPressed) PillTabBarDefaults.PressedScale else 1f,
    animationSpec = PillTabBarDefaults.PressAnimationSpec,
    label = "pillTabScale"
)

private fun transformOriginForIndex(index: Int, count: Int) = TransformOrigin(
    pivotFractionX = when (index) {
        0 -> 0f
        count - 1 -> 1f
        else -> 0.5f
    },
    pivotFractionY = 0.5f
)

object PillTabBarDefaults {
    val ContainerHeight: Dp = 48.dp
    val ContainerPadding: Dp = 4.dp
    val IconTextSpacing: Dp = 6.dp

    internal const val PressedScale = 0.99f
    internal val PressAnimationSpec = tween<Float>(
        durationMillis = 80,
        easing = CubicBezierEasing(0.2f, 0f, 0.4f, 1f)
    )

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        indicatorColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = PillTabBarColors(
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor
    )
}

@Immutable
data class PillTabBarColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color
)

@Immutable
private data class PillTabBarState(
    val pagerState: PagerState,
    val tabCount: Int,
    val pressedTabIndex: Int,
    val onPressedTabIndexChange: (Int) -> Unit
)

private val LocalPillTabBarState = staticCompositionLocalOf<PillTabBarState> {
    error("PillTab must be used within PillTabBar")
}

private val LocalPillTabBarColors = staticCompositionLocalOf<PillTabBarColors> {
    error("PillTab must be used within PillTabBar")
}
