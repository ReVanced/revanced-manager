package app.revanced.manager.ui.screen.onboarding

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.revanced.manager.R
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.SettingsListItem

@Composable
fun PermissionsStepContent(
    canInstallUnknownApps: Boolean,
    isNotificationsEnabled: Boolean,
    isBatteryOptimizationExempt: Boolean,
    onRequestInstallApps: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val buttonText = stringResource(R.string.permission_grant)
    val textStyle = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current

    val minButtonWidth = remember(buttonText, textStyle, density) {
        val textWidthPx = textMeasurer.measure(buttonText, textStyle).size.width
        max(64.dp, with(density) { textWidthPx.toDp() } + 24.dp)
    }

    ListSection(contentPadding = PaddingValues(0.dp)) {
        PermissionItem(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.permission_install_apps),
            description = stringResource(R.string.permission_install_apps_description),
            isGranted = canInstallUnknownApps,
            minTrailingWidth = minButtonWidth,
            onRequest = onRequestInstallApps
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.permission_notifications),
                description = stringResource(R.string.permission_notifications_description),
                isGranted = isNotificationsEnabled,
                minTrailingWidth = minButtonWidth,
                onRequest = onRequestNotifications
            )
        }

        PermissionItem(
            icon = Icons.Outlined.BatteryAlert,
            title = stringResource(R.string.permission_battery),
            description = stringResource(R.string.permission_battery_description),
            isGranted = isBatteryOptimizationExempt,
            minTrailingWidth = minButtonWidth,
            onRequest = onRequestBatteryOptimization
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    minTrailingWidth: Dp,
    onRequest: () -> Unit
) {
    SettingsListItem(
        onClick = if (isGranted) null else onRequest,
        headlineContent = title,
        supportingContent = description,
        leadingContent = {
            OnboardingLeadingIcon(
                icon = icon,
                containerColor = if (isGranted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                iconColor = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier.defaultMinSize(minWidth = minTrailingWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedContent(
                    targetState = isGranted,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.Center,
                    label = "onboarding_trailing_button"
                ) { isGranted ->
                    if (isGranted) {
                        OnboardingLeadingIcon(
                            icon = Icons.Default.Check,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 32.dp,
                            iconSize = 16.dp
                        )
                    } else {
                        FilledTonalButton(
                            onClick = onRequest,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(
                                text = stringResource(R.string.permission_grant),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
internal fun OnboardingLeadingIcon(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    val animatedContainerColor by animateColorAsState(
        containerColor,
        tween(),
        "containerColor",
    )
    val animatedIconColor by animateColorAsState(
        iconColor,
        tween(),
        "iconColor",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(animatedContainerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = animatedIconColor
        )
    }
}
