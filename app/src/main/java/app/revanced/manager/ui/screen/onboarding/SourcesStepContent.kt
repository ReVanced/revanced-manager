package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TrustDialog
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.ApiDownloaderState

@Composable
fun SourcesStepContent(
    apiDownloaderState: ApiDownloaderState,
    apiDownloaderProgress: Float,
    apiDownloaderIsUpdate: Boolean,
    apiDownloaderIsTrusted: Boolean,
    apiDownloaderName: String?,
    apiDownloaderSignature: String?,
    onInstallApiDownloader: () -> Unit,
    onRetryApiDownloader: () -> Unit,
    onTrustApiDownloader: () -> Unit,
) {
    ListSection(contentPadding = PaddingValues(0.dp)) {
        when (apiDownloaderState) {
            ApiDownloaderState.CHECKING -> {
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_downloader),
                    supportingContent = stringResource(R.string.api_downloader_checking),
                    leadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                )
            }

            ApiDownloaderState.AVAILABLE -> {
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_downloader),
                    supportingContent = stringResource(
                        if (apiDownloaderIsUpdate) R.string.api_downloader_update_available
                        else R.string.api_downloader_available
                    ),
                    onClick = onInstallApiDownloader,
                    leadingContent = {
                        OnboardingLeadingIcon(
                            icon = Icons.Outlined.Download,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }

            ApiDownloaderState.DOWNLOADING -> {
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_downloader),
                    supportingContent = stringResource(
                        if (apiDownloaderIsUpdate) R.string.api_downloader_updating
                        else R.string.api_downloader_downloading
                    ),
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center) {
                            OnboardingLeadingIcon(
                                icon = Icons.Outlined.Download,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            CircularProgressIndicator(
                                progress = { apiDownloaderProgress },
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }

            ApiDownloaderState.INSTALLING -> {
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_downloader),
                    supportingContent = stringResource(R.string.api_downloader_installing),
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center) {
                            OnboardingLeadingIcon(
                                icon = Icons.Outlined.Download,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }

            ApiDownloaderState.UP_TO_DATE -> {
                var showTrustDialog by rememberSaveable { mutableStateOf(false) }

                if (showTrustDialog && !apiDownloaderIsTrusted && apiDownloaderName != null && apiDownloaderSignature != null) {
                    TrustDialog(
                        title = R.string.downloader_trust_dialog_title,
                        body = stringResource(R.string.downloader_trust_dialog_body),
                        downloaderName = apiDownloaderName,
                        signature = apiDownloaderSignature,
                        onDismiss = { showTrustDialog = false },
                        onConfirm = { onTrustApiDownloader() }
                    )
                }

                if (apiDownloaderIsTrusted) {
                    SettingsListItem(
                        headlineContent = stringResource(R.string.api_downloader),
                        supportingContent = stringResource(R.string.api_downloader_up_to_date),
                        leadingContent = {
                            OnboardingLeadingIcon(
                                icon = Icons.Outlined.Verified,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                } else {
                    SettingsListItem(
                        headlineContent = stringResource(R.string.api_downloader),
                        supportingContent = stringResource(R.string.downloader_state_untrusted_tap_to_trust),
                        onClick = { showTrustDialog = true },
                        leadingContent = {
                            OnboardingLeadingIcon(
                                icon = Icons.Outlined.GppMaybe,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                iconColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    )
                }
            }

            ApiDownloaderState.FAILED -> {
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_downloader),
                    supportingContent = stringResource(R.string.api_downloader_failed),
                    leadingContent = {
                        OnboardingLeadingIcon(
                            icon = Icons.Outlined.ErrorOutline,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            iconColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    },
                    onClick = onRetryApiDownloader
                )
            }

            else -> {}
        }
    }
}
