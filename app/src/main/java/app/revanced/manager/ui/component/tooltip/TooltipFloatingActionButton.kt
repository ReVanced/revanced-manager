package app.revanced.manager.ui.component.tooltip

import android.annotation.SuppressLint
import androidx.annotation.Discouraged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.window.PopupPositionProvider
import app.revanced.manager.ui.component.haptics.HapticFloatingActionButton

/**
 * [HapticFloatingActionButton] with tooltip-specific params.
 *
 * @param tooltip [String] text to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [HapticFloatingActionButton]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tooltip: String,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable (() -> Unit)
) {
    TooltipWrap(
        modifier = modifier,
        tooltip = tooltip,
        positionProvider = positionProvider,
        haptic = haptic,
        hapticFeedbackType = hapticFeedbackType,
    ) {
        HapticFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

/**
 * [HapticFloatingActionButton] with tooltip-specific params.
 *
 * @param tooltip [Int] or `id` string resource to show in a tooltip.
 * @param positionProvider [PopupPositionProvider] Anchor point for the tooltip.
 * @param haptic Whether to perform haptic feedback when the tooltip shown.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 *
 * @see [HapticFloatingActionButton]
 */
@SuppressLint("DiscouragedApi")
@Discouraged(
    message = "Consider using string resource for tooltip text in production to make "
            + "the text translatable."
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @StringRes tooltip: Int,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    haptic: Boolean = true,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    content: @Composable (() -> Unit)
) {
    TooltipWrap(
        modifier = modifier,
        tooltip = tooltip,
        positionProvider = positionProvider,
        haptic = haptic,
        hapticFeedbackType = hapticFeedbackType,
    ) {
        HapticFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
