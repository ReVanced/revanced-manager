package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticSwitch
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.OnboardingPluginInfo

@Composable
fun SourcesStepContent(
    plugins: List<OnboardingPluginInfo>,
    onTrustPlugin: (String) -> Unit,
    onRevokePluginTrust: (String) -> Unit,
    showSubtitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    ColumnWithScrollbarEdgeShadow(modifier = modifier.fillMaxSize()) {
        if (showSubtitle) {
            Text(
                text = stringResource(R.string.onboarding_sources_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (plugins.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_no_sources),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ListSection {
                plugins.forEach { plugin ->
                    SettingsListItem(
                        headlineContent = plugin.name,
                        supportingContent = plugin.version,
                        leadingContent = {
                            OnboardingLeadingIcon(
                                icon = Icons.Outlined.Download,
                                containerColor = if (plugin.isTrusted) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                iconColor = if (plugin.isTrusted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        trailingContent = {
                            HapticSwitch(
                                checked = plugin.isTrusted,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onTrustPlugin(plugin.packageName)
                                    } else {
                                        onRevokePluginTrust(plugin.packageName)
                                    }
                                }
                            )
                        },
                        onClick = {
                            if (plugin.isTrusted) {
                                onRevokePluginTrust(plugin.packageName)
                            } else {
                                onTrustPlugin(plugin.packageName)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_sources_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
