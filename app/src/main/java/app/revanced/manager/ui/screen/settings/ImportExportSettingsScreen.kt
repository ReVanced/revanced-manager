package app.revanced.manager.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.domain.manager.KeystoreManager.Companion.DEFAULT
import app.revanced.manager.domain.manager.KeystoreManager.Companion.FLUTTER_MANAGER_PASSWORD
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FileSelector
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.sources.SourceSelector
import org.koin.androidx.compose.getViewModel
import org.koin.compose.rememberKoinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSettingsScreen(
    onBackClick: () -> Unit,
    vm: ImportExportViewModel = getViewModel()
) {
    var showImportKeystoreDialog by rememberSaveable { mutableStateOf(false) }
    var showExportKeystoreDialog by rememberSaveable { mutableStateOf(false) }

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

    if (showImportKeystoreDialog) {
        ImportKeystoreDialog(
            onDismissRequest = { showImportKeystoreDialog = false },
            onImport = vm::importKeystore
        )
    }
    if (showExportKeystoreDialog) {
        ExportKeystoreDialog(
            onDismissRequest = { showExportKeystoreDialog = false },
            onExport = vm::exportKeystore
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
                    showImportKeystoreDialog = true
                },
                headline = R.string.import_keystore,
                description = R.string.import_keystore_descripion
            )
            GroupItem(
                onClick = {
                    showExportKeystoreDialog = true
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
fun ExportKeystoreDialog(
    onDismissRequest: () -> Unit,
    onExport: (Uri) -> Unit
) {
    val activityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            uri?.let {
                onExport(it)
                onDismissRequest()
            }
        }
    val prefs: PreferencesManager = rememberKoinInject()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = { activityLauncher.launch("Manager.keystore") }
            ) {
                Text(stringResource(R.string.select_file))
            }
        },
        title = { Text(stringResource(R.string.export_keystore)) },
        text = {
            Column {
                Text("Current common name: ${prefs.keystoreCommonName}")
                Text("Current password: ${prefs.keystorePass}")
            }
        }
    )
}

@Composable
fun ImportKeystoreDialog(
    onDismissRequest: () -> Unit, onImport: (Uri, String, String) -> Unit
) {
    var cn by rememberSaveable { mutableStateOf(DEFAULT) }
    var pass by rememberSaveable { mutableStateOf(DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            FileSelector(
                mime = "*/*",
                onSelect = {
                    onImport(it, cn, pass)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.select_file))
            }
        },
        title = { Text(stringResource(R.string.import_keystore)) },
        text = {
            Column {
                TextField(
                    value = cn,
                    onValueChange = { cn = it },
                    label = { Text("Common Name") }
                )
                TextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") }
                )

                Text("Credential presets")

                Button(
                    onClick = {
                        cn = DEFAULT
                        pass = DEFAULT
                    }
                ) {
                    Text(stringResource(R.string.import_keystore_preset_default))
                }
                Button(
                    onClick = {
                        cn = DEFAULT
                        pass = FLUTTER_MANAGER_PASSWORD
                    }
                ) {
                    Text(stringResource(R.string.import_keystore_preset_flutter))
                }
            }
        }
    )
}