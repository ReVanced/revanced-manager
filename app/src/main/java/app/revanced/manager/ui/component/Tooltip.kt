package app.revanced.manager.ui.component

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView

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
    val tooltipState = rememberTooltipState()

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
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
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

