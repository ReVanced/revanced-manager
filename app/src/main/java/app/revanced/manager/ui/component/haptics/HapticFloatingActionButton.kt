package app.revanced.manager.ui.component.haptics

import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FloatingActionButton
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
fun HapticFloatingActionButton (
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String? = null,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    TooltipHost(tooltip = tooltip, modifier = modifier) { tooltipModifier ->
        FloatingActionButton(
            onClick = onClick.withHapticFeedback(HapticFeedbackConstantsCompat.VIRTUAL_KEY),
            modifier = tooltipModifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource,
            content = content
        )
    }
}
