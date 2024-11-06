package app.revanced.manager.ui.screen.settings.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.settings.Changelog
import app.revanced.manager.ui.viewmodel.UpdateViewModel
import app.revanced.manager.ui.viewmodel.UpdateViewModel.Changelog
import app.revanced.manager.ui.viewmodel.UpdateViewModel.State
import app.revanced.manager.util.formatNumber
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
        }
    ) { paddingValues ->
        AnimatedVisibility(visible = vm.showInternetCheckDialog) {
            MeteredDownloadConfirmationDialog(
                onDismiss = { vm.showInternetCheckDialog = false },
                onDownloadAnyways = { vm.downloadUpdate(true) }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 16.dp, horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Header(
                vm.state,
                vm.changelog,
                DownloadData(vm.downloadProgress, vm.downloadedSize, vm.totalSize)
            )
            vm.changelog?.let { changelog ->
                HorizontalDivider()
                Changelog(changelog)
            } ?: Spacer(modifier = Modifier.weight(1f))
            Buttons(vm.state, vm::downloadUpdate, vm::installUpdate, onBackClick)
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
private fun Header(state: State, changelog: Changelog?, downloadData: DownloadData) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(state.title),
            style = MaterialTheme.typography.headlineMedium
        )
        if (state == State.CAN_DOWNLOAD) {
            Column {
                Text(
                    text = stringResource(
                        id = R.string.current_version,
                        BuildConfig.VERSION_NAME
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                changelog?.let { changelog ->
                    Text(
                        text = stringResource(
                            id = R.string.new_version,
                            changelog.version.replace("v", "")
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state == State.DOWNLOADING) {
            LinearProgressIndicator(
                progress = { downloadData.downloadProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text =
                "${downloadData.downloadedSize.div(1000000)} MB /  ${
                    downloadData.totalSize.div(
                        1000000
                    )
                } MB (${
                    downloadData.downloadProgress.times(
                        100
                    ).toInt()
                }%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ColumnScope.Changelog(changelog: Changelog) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
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
            markdown = changelog.body.replace("`", ""),
            version = changelog.version,
            downloadCount = changelog.downloadCount.formatNumber(),
            publishDate = changelog.publishDate.relativeTime(LocalContext.current)
        )
    }
}

@Composable
private fun Buttons(
    state: State,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (state.showCancel) {
            TextButton(
                onClick = onBackClick,
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (state == State.CAN_DOWNLOAD) {
            Button(onClick = onDownloadClick) {
                Text(text = stringResource(R.string.update))
            }
        } else if (state == State.CAN_INSTALL) {
            Button(
                onClick = onInstallClick
            ) {
                Text(text = stringResource(R.string.install_app))
            }
        }
    }
}

data class DownloadData(
    val downloadProgress: Float,
    val downloadedSize: Long,
    val totalSize: Long
)