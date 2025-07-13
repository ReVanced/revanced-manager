package app.revanced.manager.ui.component.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
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
            val useNewConstants = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            val hapticFeedbackType = when {
                newChecked && useNewConstants -> HapticFeedbackConstants.TOGGLE_ON
                newChecked -> HapticFeedbackConstants.VIRTUAL_KEY
                !newChecked && useNewConstants -> HapticFeedbackConstants.TOGGLE_OFF
                !newChecked -> HapticFeedbackConstants.CLOCK_TICK
                else -> {HapticFeedbackConstants.VIRTUAL_KEY}
            }
            view.performHapticFeedback(hapticFeedbackType)
            onCheckedChange(newChecked)
        },
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}
