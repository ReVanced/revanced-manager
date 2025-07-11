package app.revanced.manager.ui.component.tooltip

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Wraps a composable with a tooltip.
 *
 * @param modifier the [Modifier] to applied to Tooltip.
 * @param tooltip [String] text to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param content The composable UI to wrapped with.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [TooltipBox]
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TooltipWrap(
    modifier: Modifier,
    tooltip: String,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val localHaptic = LocalHapticFeedback.current

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible && haptic) {
            localHaptic.performHapticFeedback(hapticFeedbackType)
        }
    }

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = tooltipState,
        content = content,
    )
}

/**
 * Wraps a composable with a tooltip.
 *
 * @param modifier the [Modifier] to applied to tooltip.
 * @param tooltip [Int] or `id` string resource to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param content The composable UI to wrapped with.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [TooltipBox]
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TooltipWrap(
    modifier: Modifier,
    @StringRes tooltip: Int,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val localHaptic = LocalHapticFeedback.current

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible && haptic) {
            localHaptic.performHapticFeedback(hapticFeedbackType)
        }
    }

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = { PlainTooltip { Text(stringResource(tooltip)) } },
        state = tooltipState,
        content = content,
    )
}
