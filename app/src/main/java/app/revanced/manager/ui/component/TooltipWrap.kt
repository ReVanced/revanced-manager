package app.revanced.manager.ui.component

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
 * @param modifier the [Modifier] to be applied to the [TooltipBox]
 * @param tooltip the text to be displayed in the [TooltipBox]
 * @param positionProvider Anchor point for the tooltip, defaults to [TooltipDefaults.rememberPlainTooltipPositionProvider]
 * @param content The composable UI to be wrapped with [TooltipBox]
 *
 * @see [TooltipBox]
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TooltipWrap(
    modifier: Modifier,
    tooltip: String,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    content: @Composable () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            PlainTooltip { Text(tooltip) }
        },
        state = tooltipState
    ) {
        content()
    }
}

/**
 * Wraps a composable with a tooltip.
 *
 * @param modifier the [Modifier] to be applied to the [TooltipBox]
 * @param tooltip the R.string to be displayed in the [TooltipBox]
 * @param positionProvider Anchor point for the tooltip, defaults to [TooltipDefaults.rememberPlainTooltipPositionProvider]
 * @param content The composable UI to be wrapped with [TooltipBox]
 *
 * @see [TooltipBox]
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TooltipWrap(
    modifier: Modifier,
    @StringRes tooltip: Int,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    content: @Composable () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            PlainTooltip { Text(stringResource(tooltip)) }
        },
        state = tooltipState
    ) {
        content()
    }
}
