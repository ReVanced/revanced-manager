package app.revanced.manager.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.PasswordField
import app.revanced.manager.ui.component.bundle.BundleSelector
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.viewmodel.ResetDialogState
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportExportSettingsScreen(
    onBackClick: () -> Unit,
    vm: ImportExportViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var showResetSheet by rememberSaveable { mutableStateOf(false) }

    val importKeystoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let { uri -> vm.startKeystoreImport(uri) }
        }
    val exportKeystoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            it?.let(vm::exportKeystore)
        }

    val patchBundles by vm.patchBundles.collectAsStateWithLifecycle(initialValue = emptyList())

    vm.selectionAction?.let { action ->
        val launcher = rememberLauncherForActivityResult(action.activityContract) { uri ->
            if (uri == null) {
                vm.clearSelectionAction()
            } else {
                vm.executeSelectionAction(uri)
            }
        }

        if (vm.selectedBundle == null) {
            BundleSelector(patchBundles) {
                if (it == null) {
                    vm.clearSelectionAction()
                } else {
                    vm.selectBundle(it)
                    launcher.launch(action.activityArg)
                }
            }
        }
    }

    if (vm.showCredentialsDialog) {
        KeystoreCredentialsDialog(
            onDismissRequest = vm::cancelKeystoreImport,
            onSubmit = { alias, pass ->
                vm.viewModelScope.launch {
                    uiSafe(context, R.string.failed_to_import_keystore, "Failed to import keystore") {
                        val result = vm.tryKeystoreImport(alias, pass)
                        if (!result) context.toast(resources.getString(R.string.import_keystore_wrong_credentials))
                    }
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.import_export),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListSection(
                title = stringResource(R.string.import_),
                leadingContent = { Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                GroupItem(
                    onClick = {
                        importKeystoreLauncher.launch("*/*")
                    },
                    headline = R.string.import_keystore,
                    description = R.string.import_keystore_description
                )
                GroupItem(
                    onClick = vm::importSelection,
                    headline = R.string.import_patch_selection,
                    description = R.string.import_patch_selection_description
                )
            }

            ListSection(
                title = stringResource(R.string.export),
                leadingContent = { Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
            GroupItem(
                onClick = {
                    if (!vm.canExport()) {
                        context.toast(resources.getString(R.string.export_keystore_unavailable))
                        return@GroupItem
                    }
                    exportKeystoreLauncher.launch("Manager.keystore")
                },
                headline = R.string.export_keystore,
                description = R.string.export_keystore_description
            )
                GroupItem(
                    onClick = vm::exportSelection,
                    headline = R.string.export_patch_selection,
                    description = R.string.export_patch_selection_description
                )
            }
ListSection(
                title = stringResource(R.string.reset),
                leadingContent = { Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                GroupItem(
                    onClick = {
                        vm.resetDialogState = ResetDialogState.Keystore {
                            vm.regenerateKeystore()
                        }
                    },
                    headline = R.string.regenerate_keystore,
                    description = R.string.regenerate_keystore_description
                )
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = animateColorAsState(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.motionScheme.defaultEffectsSpec(),
                    "surfaceContainerLow"
                ).value,
            ) {
                FilledTonalButton(
                    onClick = { showResetSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.reset))
                }
            }}

            if (showResetSheet) {
                ResetBottomSheet(
                    onDismiss = { showResetSheet = false },
                    onReset = { resetSelections, resetOptions ->
                        if (resetSelections) vm.resetSelection()
                        if (resetOptions) vm.resetOptions()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResetBottomSheet(
    onDismiss: () -> Unit,
    onReset: (resetSelections: Boolean, resetOptions: Boolean) -> Unit
) {
    var resetSelections by rememberSaveable { mutableStateOf(true) }
    var resetOptions by rememberSaveable { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.reset_configuration),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.large),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SegmentedListItem(
                    onClick = { resetSelections = !resetSelections },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    leadingContent = {
                        HapticCheckbox(
                            checked = resetSelections,
                            onCheckedChange = null
                        )
                    },
                ) {
                    Text(stringResource(R.string.reset_patch_selection))
                }
                SegmentedListItem(
                    onClick = { resetOptions = !resetOptions },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    leadingContent = {
                        HapticCheckbox(
                            checked = resetOptions,
                            onCheckedChange = null
                        )
                    },
                ) {
                    Text(stringResource(R.string.reset_patch_options))
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                    onReset(resetSelections, resetOptions)
                },
                enabled = resetSelections || resetOptions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Composable
private fun GroupItem(
    onClick: () -> Unit,
    @StringRes headline: Int,
    @StringRes description: Int? = null
) {
    SettingsListItem(
        onClick = onClick,
        headlineContent = stringResource(headline),
        supportingContent = description?.let { stringResource(it) }
    )
}

@Composable
fun KeystoreCredentialsDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var alias by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(alias, pass)
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_keystore_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
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