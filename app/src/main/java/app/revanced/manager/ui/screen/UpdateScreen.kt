package app.revanced.manager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.settings.Changelog
import app.revanced.manager.ui.viewmodel.UpdateViewModel
import app.revanced.manager.ui.viewmodel.UpdateViewModel.State
import app.revanced.manager.util.relativeTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Stable
fun UpdateScreen(
    onBackClick: () -> Unit,
    vm: UpdateViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val buttonConfig = when (vm.state) {
        State.CAN_DOWNLOAD -> Triple(
            { vm.downloadUpdate() },
            R.string.download,
            Icons.Outlined.InstallMobile
        )
        State.DOWNLOADING -> Triple(onBackClick, R.string.cancel, Icons.Outlined.Cancel)
        State.CAN_INSTALL -> Triple(
            { vm.installUpdate() },
            R.string.install_app,
            Icons.Outlined.InstallMobile
        )
        else -> null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(stringResource(vm.state.title))

                        if (vm.state == State.DOWNLOADING) {
                            Text(
                                text = "${vm.downloadedSize.div(1000000)} MB /  ${
                                    vm.totalSize.div(1000000)
                                } MB (${vm.downloadProgress.times(100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            buttonConfig?.let { (onClick, textRes, icon) ->
                BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onClick::invoke
                    ) {
                        Icon(icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(textRes))
                    }
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            if (vm.state == State.DOWNLOADING)
                LinearProgressIndicator(
                    progress = { vm.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )

            AnimatedVisibility(visible = vm.showInternetCheckDialog) {
                MeteredDownloadConfirmationDialog(
                    onDismiss = { vm.showInternetCheckDialog = false },
                    onDownloadAnyways = { vm.downloadUpdate(true) }
                )
            }

            vm.releaseInfo?.let { changelog ->
                Changelog(changelog)
            }
        }
    }
}

@Composable
private fun MeteredDownloadConfirmationDialog(
    onDismiss: () -> Unit,
    onDownloadAnyways: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onDownloadAnyways()
                }
            ) {
                Text(stringResource(R.string.download))
            }
        },
        title = { Text(stringResource(R.string.download_update_confirmation)) },
        icon = { Icon(Icons.Outlined.Update, null) },
        text = { Text(stringResource(R.string.download_confirmation_metered)) }
    )
}

@Composable
private fun ColumnScope.Changelog(releaseInfo: ReVancedAsset) {
    ColumnWithScrollbarEdgeShadow(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Changelog(
            markdown = releaseInfo.description.replace("`", ""),
            version = releaseInfo.version,
            publishDate = releaseInfo.createdAt.relativeTime(LocalContext.current)
        )
    }
}