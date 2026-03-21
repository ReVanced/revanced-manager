package app.revanced.manager.ui.screen.onboarding

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.SettingsListItem

@Composable
fun PermissionsStepContent(
    canInstallUnknownApps: Boolean,
    isNotificationsEnabled: Boolean,
    isBatteryOptimizationExempt: Boolean,
    isShizukuAvailable: Boolean,
    isShizukuAuthorized: Boolean,
    isAdbConnected: Boolean,
    onRequestInstallApps: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestAdb: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListSection(
            title = stringResource(R.string.permissions),
            leadingContent = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            PermissionItem(
                icon = Icons.Outlined.Security,
                title = stringResource(R.string.permission_install_apps),
                description = stringResource(R.string.permission_install_apps_description),
                isGranted = canInstallUnknownApps,
                onRequest = onRequestInstallApps
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.permission_notifications),
                    description = stringResource(R.string.permission_notifications_description),
                    isGranted = isNotificationsEnabled,
                    onRequest = onRequestNotifications
                )
            }

            PermissionItem(
                icon = Icons.Outlined.BatteryAlert,
                title = stringResource(R.string.permission_battery),
                description = stringResource(R.string.permission_battery_description),
                isGranted = isBatteryOptimizationExempt,
                onRequest = onRequestBatteryOptimization
            )
        }

        ListSection(
            title = stringResource(R.string.category_installer),
            leadingContent = {
                Icon(
                    Icons.Outlined.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            PermissionItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_shizuku),
                title = stringResource(R.string.permission_shizuku),
                description = stringResource(R.string.permission_shizuku_description),
                isGranted = isShizukuAuthorized,
                onRequest = onRequestShizuku
            )

            PermissionItem(
                icon = Icons.Outlined.Terminal,
                title = stringResource(R.string.permission_adb),
                description = stringResource(R.string.permission_adb_description),
                isGranted = isAdbConnected,
                onRequest = onRequestAdb
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
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
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconColor
        )
    }
}
