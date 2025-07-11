package app.revanced.manager.ui.component.tooltip

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.window.PopupPositionProvider

/**
 * [IconButton] with tooltip-specific params.
 *
 * @param tooltip [String] text to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [IconButton]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tooltip: String,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable (() -> Unit),
) {
    TooltipWrap(
        modifier = modifier,
        tooltip = tooltip,
        positionProvider = positionProvider,
        haptic = haptic,
        hapticFeedbackType = hapticFeedbackType,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

/**
 * [IconButton] with tooltip-specific params.
 *
 * @param tooltip [Int] or `id` string resource to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [IconButton]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @StringRes tooltip: Int,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable (() -> Unit),
) {
    TooltipWrap(
        modifier = modifier,
        tooltip = tooltip,
        positionProvider = positionProvider,
        haptic = haptic,
        hapticFeedbackType = hapticFeedbackType,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
