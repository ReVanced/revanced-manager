package app.revanced.manager.ui.component.haptics

import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView

@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val view = LocalView.current
    Switch(
        checked = checked,
        onCheckedChange = { newChecked ->
            view.performHapticFeedback(
                if (newChecked) HapticFeedbackConstantsCompat.TOGGLE_ON else HapticFeedbackConstantsCompat.TOGGLE_OFF
            )
            onCheckedChange(newChecked)
        },
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}
