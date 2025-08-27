package app.revanced.manager.ui.component

import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private val properties = DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnBackPress = true,
    decorFitsSystemWindows = false,
)

@Composable
fun FullscreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val view = LocalView.current
        val isDarkTheme = isSystemInDarkTheme()
        LaunchedEffect(isDarkTheme) {
            val window = (view.parent as DialogWindowProvider).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }

        content()
    }
}