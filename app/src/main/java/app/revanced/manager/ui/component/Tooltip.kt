package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

// 12.dp is visually fine, however 16.dp is the exact padding of all pages
private val TooltipScreenPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipHost(
    tooltip: String?,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    if (tooltip.isNullOrBlank()) {
        content(modifier)
        return
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val tooltipState = rememberTooltipState()
    val preferredPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)
    val alternatePositionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below)
    val screenPadding = with(density) { TooltipScreenPadding.roundToPx() }
    val leftBound = WindowInsets.safeDrawing.getLeft(density, layoutDirection) + screenPadding
    val topBound = WindowInsets.safeDrawing.getTop(density) + screenPadding
    val rightInset = WindowInsets.safeDrawing.getRight(density, layoutDirection) + screenPadding
    val bottomInset = WindowInsets.safeDrawing.getBottom(density) + screenPadding
    val safeTooltipPositionProvider = remember(
        preferredPositionProvider,
        alternatePositionProvider,
        leftBound,
        topBound,
        rightInset,
        bottomInset,
    ) {
        SafeAreaTooltipPositionProvider(
            preferredPositionProvider = preferredPositionProvider,
            alternatePositionProvider = alternatePositionProvider,
            leftBound = leftBound,
            topBound = topBound,
            rightInset = rightInset,
            bottomInset = bottomInset,
        )
    }

    LaunchedEffect(tooltipState, view) {
        var wasVisible = tooltipState.isVisible

        snapshotFlow { tooltipState.isVisible }.collect { isVisible ->
            if (isVisible && !wasVisible) {
                view.performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
            }

            wasVisible = isVisible
        }
    }

    TooltipBox(
        positionProvider = safeTooltipPositionProvider,
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = tooltipState,
    ) {
        content(modifier)
    }
}

private class SafeAreaTooltipPositionProvider(
    private val preferredPositionProvider: PopupPositionProvider,
    private val alternatePositionProvider: PopupPositionProvider,
    private val leftBound: Int,
    private val topBound: Int,
    private val rightInset: Int,
    private val bottomInset: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val preferredOffset = preferredPositionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = popupContentSize
        )
        val alternateOffset = alternatePositionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = popupContentSize
        )

        val rightLimit = windowSize.width - rightInset
        val bottomLimit = windowSize.height - bottomInset
        val maxX = maxOf(leftBound, rightLimit - popupContentSize.width)
        val maxY = maxOf(topBound, bottomLimit - popupContentSize.height)

        fun fitsVertically(offset: IntOffset): Boolean {
            val top = offset.y
            val bottom = offset.y + popupContentSize.height
            return top >= topBound && bottom <= bottomLimit
        }

        val targetOffset = when {
            fitsVertically(preferredOffset) -> preferredOffset
            fitsVertically(alternateOffset) -> alternateOffset
            else -> preferredOffset
        }

        return IntOffset(
            x = targetOffset.x.coerceIn(leftBound, maxX),
            y = targetOffset.y.coerceIn(topBound, maxY)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    tooltip: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (String?) -> Unit,
) {
    TooltipHost(tooltip = tooltip, modifier = modifier) { tooltipModifier ->
        IconButton(
            onClick = onClick,
            modifier = tooltipModifier,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
        ) {
            content(tooltip)
        }
    }
}

