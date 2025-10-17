package app.revanced.manager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.revanced.manager.R

private val DarkColorScheme = darkColorScheme(
    primary = rv_theme_dark_primary,
    onPrimary = rv_theme_dark_onPrimary,
    primaryContainer = rv_theme_dark_primaryContainer,
    onPrimaryContainer = rv_theme_dark_onPrimaryContainer,
    secondary = rv_theme_dark_secondary,
    onSecondary = rv_theme_dark_onSecondary,
    secondaryContainer = rv_theme_dark_secondaryContainer,
    onSecondaryContainer = rv_theme_dark_onSecondaryContainer,
    tertiary = rv_theme_dark_tertiary,
    onTertiary = rv_theme_dark_onTertiary,
    tertiaryContainer = rv_theme_dark_tertiaryContainer,
    onTertiaryContainer = rv_theme_dark_onTertiaryContainer,
    error = rv_theme_dark_error,
    errorContainer = rv_theme_dark_errorContainer,
    onError = rv_theme_dark_onError,
    onErrorContainer = rv_theme_dark_onErrorContainer,
    background = rv_theme_dark_background,
    onBackground = rv_theme_dark_onBackground,
    surface = rv_theme_dark_surface,
    onSurface = rv_theme_dark_onSurface,
    surfaceVariant = rv_theme_dark_surfaceVariant,
    onSurfaceVariant = rv_theme_dark_onSurfaceVariant,
    outline = rv_theme_dark_outline,
    inverseOnSurface = rv_theme_dark_inverseOnSurface,
    inverseSurface = rv_theme_dark_inverseSurface,
    inversePrimary = rv_theme_dark_inversePrimary,
    surfaceTint = rv_theme_dark_surfaceTint,
    outlineVariant = rv_theme_dark_outlineVariant,
    scrim = rv_theme_dark_scrim,
)

private val LightColorScheme = lightColorScheme(
    primary = rv_theme_light_primary,
    onPrimary = rv_theme_light_onPrimary,
    primaryContainer = rv_theme_light_primaryContainer,
    onPrimaryContainer = rv_theme_light_onPrimaryContainer,
    secondary = rv_theme_light_secondary,
    onSecondary = rv_theme_light_onSecondary,
    secondaryContainer = rv_theme_light_secondaryContainer,
    onSecondaryContainer = rv_theme_light_onSecondaryContainer,
    tertiary = rv_theme_light_tertiary,
    onTertiary = rv_theme_light_onTertiary,
    tertiaryContainer = rv_theme_light_tertiaryContainer,
    onTertiaryContainer = rv_theme_light_onTertiaryContainer,
    error = rv_theme_light_error,
    errorContainer = rv_theme_light_errorContainer,
    onError = rv_theme_light_onError,
    onErrorContainer = rv_theme_light_onErrorContainer,
    background = rv_theme_light_background,
    onBackground = rv_theme_light_onBackground,
    surface = rv_theme_light_surface,
    onSurface = rv_theme_light_onSurface,
    surfaceVariant = rv_theme_light_surfaceVariant,
    onSurfaceVariant = rv_theme_light_onSurfaceVariant,
    outline = rv_theme_light_outline,
    inverseOnSurface = rv_theme_light_inverseOnSurface,
    inverseSurface = rv_theme_light_inverseSurface,
    inversePrimary = rv_theme_light_inversePrimary,
    surfaceTint = rv_theme_light_surfaceTint,
    outlineVariant = rv_theme_light_outlineVariant,
    scrim = rv_theme_light_scrim,
)

@Composable
fun ReVancedManagerTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlackTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.let {
        if (darkTheme && pureBlackTheme)
            it.copy(background = Color.Black, surface = Color.Black)
        else it
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity

            WindowCompat.setDecorFitsSystemWindows(activity.window, false)

            activity.window.statusBarColor = Color.Transparent.toArgb()
            activity.window.navigationBarColor = Color.Transparent.toArgb()

            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

enum class Theme(val displayName: Int) {
    SYSTEM(R.string.system),
    LIGHT(R.string.light),
    DARK(R.string.dark);
}