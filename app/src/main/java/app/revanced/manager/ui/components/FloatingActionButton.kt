package app.revanced.manager.ui.components

import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


@Composable
fun FloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val context = LocalContext.current

    // TODO: set icon color:
    // tint = if (enabled) LocalContentColor.current.copy(alpha = LocalContentAlpha.current) else else DarkGray
    CompositionLocalProvider(
        LocalRippleTheme provides if (enabled) {
            LocalRippleTheme.current
        } else NoRippleTheme
    ) {
        ExtendedFloatingActionButton(
            text = text,
            icon = icon,
            onClick = {
                if (!enabled) {
                    context.showToast("Please select an application to patch")
                }
                if (enabled) onClick()
            },
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else Color.Gray,
        )
    }
}

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}