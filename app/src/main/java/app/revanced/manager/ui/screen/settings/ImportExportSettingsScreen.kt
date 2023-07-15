package app.revanced.manager.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.PasswordField
import app.revanced.manager.ui.component.sources.SourceSelector
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSettingsScreen(
    onBackClick: () -> Unit,
    vm: ImportExportViewModel = getViewModel()
) {
    val context = LocalContext.current

    val importKeystoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let { uri -> vm.startKeystoreImport(uri) }
        }
    val exportKeystoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            it?.let(vm::exportKeystore)
        }

    vm.selectionAction?.let { action ->
        val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())
        val launcher = rememberLauncherForActivityResult(action.activityContract) { uri ->
            if (uri == null) {
                vm.clearSelectionAction()
            } else {
                vm.executeSelectionAction(uri)
            }
        }

        if (vm.selectedSource == null) {
            SourceSelector(sources) {
                if (it == null) {
                    vm.clearSelectionAction()
                } else {
                    vm.selectSource(it)
                    launcher.launch(action.activityArg)
                }
            }
        }
    }

    if (vm.showCredentialsDialog) {
        KeystoreCredentialsDialog(
            onDismissRequest = vm::cancelKeystoreImport,
            onSubmit = { cn, pass ->
                vm.viewModelScope.launch {
                    val result = vm.tryKeystoreImport(cn, pass)
                    if (!result) context.toast(context.getString(R.string.import_keystore_wrong_credentials))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.import_export),
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
            GroupHeader(stringResource(R.string.signing))
            GroupItem(
                onClick = {
                    importKeystoreLauncher.launch("*/*")
                },
                headline = R.string.import_keystore,
                description = R.string.import_keystore_descripion
            )
            GroupItem(
                onClick = {
                    if (!vm.canExport()) {
                        context.toast(context.getString(R.string.export_keystore_unavailable))
                        return@GroupItem
                    }
                    exportKeystoreLauncher.launch("Manager.keystore")
                },
                headline = R.string.export_keystore,
                description = R.string.export_keystore_description
            )
            GroupItem(
                onClick = vm::regenerateKeystore,
                headline = R.string.regenerate_keystore,
                description = R.string.regenerate_keystore_description
            )

            GroupHeader(stringResource(R.string.patches_selection))
            GroupItem(
                onClick = vm::importSelection,
                headline = R.string.restore_patches_selection,
                description = R.string.restore_patches_selection_description
            )
            GroupItem(
                onClick = vm::exportSelection,
                headline = R.string.backup_patches_selection,
                description = R.string.backup_patches_selection_description
            )
            GroupItem(
                onClick = vm::resetSelection,
                headline = R.string.clear_patches_selection,
                description = R.string.clear_patches_selection_description
            )
        }
    }
}

@Composable
private fun GroupItem(onClick: () -> Unit, @StringRes headline: Int, @StringRes description: Int) =
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(stringResource(headline)) },
        supportingContent = { Text(stringResource(description)) }
    )

@Composable
fun KeystoreCredentialsDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var cn by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(cn, pass)
                }
            ) {
                Text(stringResource(R.string.import_keystore_dialog_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(Icons.Outlined.Key, null)
        },
        title = {
            Text(
                text = stringResource(R.string.import_keystore_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_keystore_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cn,
                    onValueChange = { cn = it },
                    label = { Text(stringResource(R.string.import_keystore_dialog_alias_field)) }
                )
                PasswordField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(stringResource(R.string.import_keystore_dialog_password_field)) }
                )
            }
        }
    )
}