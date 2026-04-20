package app.revanced.manager.ui.component.haptics

import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.revanced.manager.ui.component.TooltipHost
import app.revanced.manager.util.withHapticFeedback

@Composable
fun HapticExtendedFloatingActionButton (
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String? = null,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TooltipHost(tooltip = tooltip, modifier = modifier) { tooltipModifier ->
        ExtendedFloatingActionButton(
            text = text,
            icon = icon,
            onClick = onClick.withHapticFeedback(HapticFeedbackConstantsCompat.VIRTUAL_KEY),
            modifier = tooltipModifier,
            expanded = expanded,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource
        )
    }
}
