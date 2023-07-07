package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.viewmodel.UpdateProgressViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Stable
fun UpdateProgressScreen(
    onBackClick: () -> Unit,
    vm: UpdateProgressViewModel = getViewModel()
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.updates),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 16.dp, horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (vm.isInstalling) stringResource(R.string.installing_manager_update) else stringResource(
                    R.string.downloading_manager_update
                ), style = MaterialTheme.typography.headlineMedium
            )
            LinearProgressIndicator(
                progress = vm.downloadProgress / 100f,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )
            Text(
                text = if (!vm.isInstalling) "${vm.downloadedSize.div(1000000)} MB /  ${
                    vm.totalSize.div(
                        1000000
                    )
                } MB (${vm.downloadProgress.toInt()}%)" else stringResource(R.string.installing_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = "This update adds many functionality and fixes many issues in Manager. New experiment toggles are also added, they can be found in Settings > Advanced. Please submit some feedback in Settings > About > Submit issues or feedback. Thank you, everyone!",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onBackClick,
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(onClick = vm::installUpdate, enabled = vm.finished) {
                    Text(text = stringResource(R.string.update))
                }
            }
        }
    }
}