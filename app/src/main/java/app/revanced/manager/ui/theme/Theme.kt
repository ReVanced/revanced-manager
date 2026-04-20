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

private val LightColorScheme = lightColorScheme(
    primary = RVThemePrimaryLight,
    onPrimary = RVThemeOnPrimaryLight,
    primaryContainer = RVThemePrimaryContainerLight,
    onPrimaryContainer = RVThemeOnPrimaryContainerLight,
    inversePrimary = RVThemeInversePrimaryLight,
    secondary = RVThemeSecondaryLight,
    onSecondary = RVThemeOnSecondaryLight,
    secondaryContainer = RVThemeSecondaryContainerLight,
    onSecondaryContainer = RVThemeOnSecondaryContainerLight,
    tertiary = RVThemeTertiaryLight,
    onTertiary = RVThemeOnTertiaryLight,
    tertiaryContainer = RVThemeTertiaryContainerLight,
    onTertiaryContainer = RVThemeOnTertiaryContainerLight,
    background = RVThemeBackgroundLight,
    onBackground = RVThemeOnBackgroundLight,
    surface = RVThemeSurfaceLight,
    onSurface = RVThemeOnSurfaceLight,
    surfaceVariant = RVThemeSurfaceVariantLight,
    onSurfaceVariant = RVThemeOnSurfaceVariantLight,
    surfaceTint = RVThemeSurfaceTintLight,
    inverseSurface = RVThemeInverseSurfaceLight,
    inverseOnSurface = RVThemeInverseOnSurfaceLight,
    error = RVThemeErrorLight,
    onError = RVThemeOnErrorLight,
    errorContainer = RVThemeErrorContainerLight,
    onErrorContainer = RVThemeOnErrorContainerLight,
    outline = RVThemeOutlineLight,
    outlineVariant = RVThemeOutlineVariantLight,
    scrim = RVThemeScrimLight,
    surfaceBright = RVThemeSurfaceBrightLight,
    surfaceContainer = RVThemeSurfaceContainerLight,
    surfaceContainerHigh = RVThemeSurfaceContainerHighLight,
    surfaceContainerHighest = RVThemeSurfaceContainerHighestLight,
    surfaceContainerLow = RVThemeSurfaceContainerLowLight,
    surfaceContainerLowest = RVThemeSurfaceContainerLowestLight,
    surfaceDim = RVThemeSurfaceDimLight,
    primaryFixed = RVThemePrimaryFixed,
    primaryFixedDim = RVThemePrimaryFixedDim,
    onPrimaryFixed = RVThemeOnPrimaryFixed,
    onPrimaryFixedVariant = RVThemeOnPrimaryFixedVariant,
    secondaryFixed = RVThemeSecondaryFixed,
    secondaryFixedDim = RVThemeSecondaryFixedDim,
    onSecondaryFixed = RVThemeOnSecondaryFixed,
    onSecondaryFixedVariant = RVThemeOnSecondaryFixedVariant,
    tertiaryFixed = RVThemeTertiaryFixed,
    tertiaryFixedDim = RVThemeTertiaryFixedDim,
    onTertiaryFixed = RVThemeOnTertiaryFixed,
    onTertiaryFixedVariant = RVThemeOnTertiaryFixedVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = RVThemePrimaryDark,
    onPrimary = RVThemeOnPrimaryDark,
    primaryContainer = RVThemePrimaryContainerDark,
    onPrimaryContainer = RVThemeOnPrimaryContainerDark,
    inversePrimary = RVThemeInversePrimaryDark,
    secondary = RVThemeSecondaryDark,
    onSecondary = RVThemeOnSecondaryDark,
    secondaryContainer = RVThemeSecondaryContainerDark,
    onSecondaryContainer = RVThemeOnSecondaryContainerDark,
    tertiary = RVThemeTertiaryDark,
    onTertiary = RVThemeOnTertiaryDark,
    tertiaryContainer = RVThemeTertiaryContainerDark,
    onTertiaryContainer = RVThemeOnTertiaryContainerDark,
    background = RVThemeBackgroundDark,
    onBackground = RVThemeOnBackgroundDark,
    surface = RVThemeSurfaceDark,
    onSurface = RVThemeOnSurfaceDark,
    surfaceVariant = RVThemeSurfaceVariantDark,
    onSurfaceVariant = RVThemeOnSurfaceVariantDark,
    surfaceTint = RVThemeSurfaceTintDark,
    inverseSurface = RVThemeInverseSurfaceDark,
    inverseOnSurface = RVThemeInverseOnSurfaceDark,
    error = RVThemeErrorDark,
    onError = RVThemeOnErrorDark,
    errorContainer = RVThemeErrorContainerDark,
    onErrorContainer = RVThemeOnErrorContainerDark,
    outline = RVThemeOutlineDark,
    outlineVariant = RVThemeOutlineVariantDark,
    scrim = RVThemeScrimDark,
    surfaceBright = RVThemeSurfaceBrightDark,
    surfaceContainer = RVThemeSurfaceContainerDark,
    surfaceContainerHigh = RVThemeSurfaceContainerHighDark,
    surfaceContainerHighest = RVThemeSurfaceContainerHighestDark,
    surfaceContainerLow = RVThemeSurfaceContainerLowDark,
    surfaceContainerLowest = RVThemeSurfaceContainerLowestDark,
    surfaceDim = RVThemeSurfaceDimDark,
    primaryFixed = RVThemePrimaryFixed,
    primaryFixedDim = RVThemePrimaryFixedDim,
    onPrimaryFixed = RVThemeOnPrimaryFixed,
    onPrimaryFixedVariant = RVThemeOnPrimaryFixedVariant,
    secondaryFixed = RVThemeSecondaryFixed,
    secondaryFixedDim = RVThemeSecondaryFixedDim,
    onSecondaryFixed = RVThemeOnSecondaryFixed,
    onSecondaryFixedVariant = RVThemeOnSecondaryFixedVariant,
    tertiaryFixed = RVThemeTertiaryFixed,
    tertiaryFixedDim = RVThemeTertiaryFixedDim,
    onTertiaryFixed = RVThemeOnTertiaryFixed,
    onTertiaryFixedVariant = RVThemeOnTertiaryFixedVariant,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
        motionScheme = MotionScheme.expressive()
    )
}

enum class Theme(val displayName: Int) {
    SYSTEM(R.string.system),
    LIGHT(R.string.light),
    DARK(R.string.dark);
}