package app.revanced.manager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.settings.Changelog
import app.revanced.manager.ui.viewmodel.UpdateViewModel
import app.revanced.manager.ui.viewmodel.UpdateViewModel.State
import app.revanced.manager.util.relativeTime
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.fill.FadingEdgesFillType
import com.gigamole.composefadingedges.verticalFadingEdges
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Stable
fun UpdateScreen(
    onBackClick: () -> Unit,
    vm: UpdateViewModel = koinViewModel()
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.update),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            BottomAppBar(
                vm.state, vm::downloadUpdate, vm::installUpdate, onBackClick,
                DownloadData(vm.downloadProgress, vm.downloadedSize, vm.totalSize)
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(vm.showInternetCheckDialog) {
            MeteredDownloadConfirmationDialog(
                onDismiss = { vm.showInternetCheckDialog = false },
                onDownloadAnyways = { vm.downloadUpdate(true) }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (vm.state in listOf(State.DOWNLOADING, State.CAN_INSTALL)) {
                LinearProgressIndicator(
                    progress = { vm.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
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
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .weight(1f)
            .verticalScroll(scrollState)
            .verticalFadingEdges(
                fillType = FadingEdgesFillType.FadeColor(
                    color = MaterialTheme.colorScheme.background,
                    fillStops = Triple(0F, 0.55F, 1F),
                    secondStopAlpha = 1F
                ),
                contentType = FadingEdgesContentType.Dynamic.Scroll(
                    state = scrollState,
                    scrollConfig = FadingEdgesScrollConfig.Dynamic(
                        animationSpec = spring(),
                        isLerpByDifferenceForPartialContent = true,
                        scrollFactor = 1.25F
                    )
                ),
                length = 350.dp
            )
    ) {
        Changelog(
            markdown = releaseInfo.description.replace("`", ""),
            version = releaseInfo.version,
            publishDate = releaseInfo.createdAt.relativeTime(LocalContext.current)
        )
    }
}

@Composable
private fun BottomAppBar(
    state: State,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onBackClick: () -> Unit,
    downloadData: DownloadData
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (state.showCancel) {
                    TextButton(onClick = onBackClick) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                when (state) {
                    State.CAN_DOWNLOAD -> Button(onClick = onDownloadClick) {
                        Text(text = stringResource(R.string.update))
                    }

                    State.CAN_INSTALL -> Button(onClick = onInstallClick) {
                        Text(text = stringResource(R.string.install_app))
                    }

                    State.DOWNLOADING -> Text(
                        text = "${downloadData.downloadedSize / 1_000_000} MB / ${downloadData.totalSize / 1_000_000} MB (${(downloadData.downloadProgress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    State.FAILED -> {

                        Button(onClick = onDownloadClick) {
                            Text(text = stringResource(R.string.try_again))
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

data class DownloadData(
    val downloadProgress: Float,
    val downloadedSize: Long,
    val totalSize: Long
)