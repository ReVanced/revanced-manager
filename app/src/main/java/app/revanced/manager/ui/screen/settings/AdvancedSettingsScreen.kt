package app.revanced.manager.ui.screen.settings

import android.app.ActivityManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.AdvancedSettingsViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit,
    vm: AdvancedSettingsViewModel = getViewModel()
) {
    val context = LocalContext.current
    val memoryLimit = remember {
        val activityManager = context.getSystemService<ActivityManager>()!!
        context.getString(
            R.string.device_memory_limit_format,
            activityManager.memoryClass,
            activityManager.largeMemoryClass
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.advanced),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val apiUrl by vm.prefs.api.getAsState()
            var showApiUrlDialog by rememberSaveable { mutableStateOf(false) }

            if (showApiUrlDialog) {
                APIUrlDialog(apiUrl) {
                    showApiUrlDialog = false
                    it?.let(vm::setApiUrl)
                }
            }
            SettingsListItem(
                headlineContent = stringResource(R.string.api_url),
                supportingContent = apiUrl,
                modifier = Modifier.clickable {
                    showApiUrlDialog = true
                }
            )

            GroupHeader(stringResource(R.string.patcher))
            BooleanItem(
                preference = vm.prefs.allowExperimental,
                coroutineScope = vm.viewModelScope,
                headline = R.string.experimental_patches,
                description = R.string.experimental_patches_description
            )
            BooleanItem(
                preference = vm.prefs.multithreadingDexFileWriter,
                coroutineScope = vm.viewModelScope,
                headline = R.string.multithreaded_dex_file_writer,
                description = R.string.multithreaded_dex_file_writer_description,
            )

            GroupHeader(stringResource(R.string.patch_bundles_section))
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_redownload),
                modifier = Modifier.clickable {
                    vm.redownloadBundles()
                }
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.patch_bundles_reset),
                modifier = Modifier.clickable {
                    vm.resetBundles()
                }
            )

            GroupHeader(stringResource(R.string.device))
            SettingsListItem(
                headlineContent = stringResource(R.string.device_model),
                supportingContent = Build.MODEL
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.device_android_version),
                supportingContent = Build.VERSION.RELEASE
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.device_architectures),
                supportingContent = Build.SUPPORTED_ABIS.joinToString(", ")
            )
            SettingsListItem(
                headlineContent = stringResource(R.string.device_memory_limit),
                supportingContent = memoryLimit
            )
        }
    }
}

@Composable
private fun APIUrlDialog(currentUrl: String, onSubmit: (String?) -> Unit) {
    var url by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(url)
                }
            ) {
                Text(stringResource(R.string.api_url_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(Icons.Outlined.Http, null)
        },
        title = {
            Text(
                text = stringResource(R.string.api_url_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.api_url_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.api_url_dialog_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.api_url)) }
                )
            }
        }
    )
}