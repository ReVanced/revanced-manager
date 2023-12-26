package app.revanced.manager.ui.component.haptics

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView

@Composable
fun HapticTab (
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val view = LocalView.current

    Tab(
        selected = selected,
        onClick = {
            // Perform haptic feedback
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        text = text,
        icon = icon,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        interactionSource = interactionSource
    )
}