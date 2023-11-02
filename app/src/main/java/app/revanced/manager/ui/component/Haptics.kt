package app.revanced.manager.ui.component

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView



@Composable
fun hapticSwitch(onClick: (Boolean) -> Unit): (Boolean) -> Unit {
    val view = LocalView.current
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
        } else {
            view.performHapticFeedback(if (it) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.CLOCK_TICK)
        }
        onClick(it)
    }
}

@Composable
fun hapticSwitch(currentValue: Boolean, onClick: () -> Unit): () -> Unit {
    val func = hapticSwitch { _ -> onClick() }
    return { func(!currentValue) }
}

@Composable
fun hapticCheckbox(onClick: (Boolean) -> Unit): (Boolean) -> Unit {
    val view = LocalView.current
    return {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick(it)
    }
}

@Composable
fun hapticCheckboxToggle(onClick: () -> Unit): () -> Unit {
    val func = hapticCheckbox { _ -> onClick() }
    return { func(false) }
}
