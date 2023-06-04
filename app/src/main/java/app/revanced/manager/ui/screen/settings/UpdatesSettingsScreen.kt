package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.destination.SettingsDestination
import app.revanced.manager.ui.viewmodel.UpdateSettingsViewModel
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesSettingsScreen(
    onBackClick: () -> Unit,
    navController: NavController<SettingsDestination>,
) {
    val listItems = listOf(
        Triple(
            stringResource(R.string.update_channel),
            stringResource(R.string.update_channel_description),
            third = { /*TODO*/ }),
        Triple(
            stringResource(R.string.update_notifications),
            stringResource(R.string.update_notifications_description),
            third = { /*TODO*/ }),
        Triple(
            stringResource(R.string.changelog),
            stringResource(R.string.changelog_description),
            third = { /*TODO*/ }),
    )


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
                .verticalScroll(rememberScrollState())
        ) {
            UpdateNotification(
                onClick = {
                    navController.navigate(SettingsDestination.UpdateProgress)
                }
            )

            listItems.forEach { (title, description, onClick) ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onClick() },
                    headlineContent = {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun UpdateNotification(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Update, contentDescription = null)
            Text(
                text = stringResource(R.string.update_notification),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProgressScreen(
    onBackClick: () -> Unit,
    vm: UpdateSettingsViewModel = getViewModel()
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
                .padding(vertical = 16.dp, horizontal = 24.dp),
        ) {
            var isInstalling by remember { mutableStateOf(false) }
            isInstalling = vm.downloadProgress >= 100

            Text(
                text = if (isInstalling) stringResource(R.string.installing_manager_update) else stringResource(
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
                text = if (!isInstalling) "${vm.downloadedSize.div(1000000)} MB /  ${vm.totalSize.div(1000000)} MB (${vm.downloadProgress.toInt()}%)" else stringResource(R.string.installing_message),
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
                    onClick = { /*TODO*/ },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(onClick = {
                    vm.installUpdate()
                }) {
                    Text(text = stringResource(R.string.update))
                }
            }
        }
    }
}