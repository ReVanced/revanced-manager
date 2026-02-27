package app.revanced.manager.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
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
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.PasswordField
import app.revanced.manager.ui.component.bundle.BundleSelector
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.viewmodel.PatchStorageStats
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
    val prefs = vm.prefs
    val clipboard = remember { context.getSystemService<ClipboardManager>()!! }
    var showResetSheet by rememberSaveable { mutableStateOf(false) }
    var showKeystorePassword by rememberSaveable { mutableStateOf(false) }
    val keystoreAlias by prefs.keystoreAlias.getAsState()
    val keystorePass by prefs.keystorePass.getAsState()

    val importKeystoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let { uri -> vm.startKeystoreImport(uri) }
        }
    val exportKeystoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            it?.let(vm::exportKeystore)
        }

    val patchBundles by vm.patchBundles.collectAsStateWithLifecycle(initialValue = emptyList())
    val patchStorageStats by vm.patchStorageStats.collectAsStateWithLifecycle(
        initialValue = PatchStorageStats()
    )

    vm.selectionAction?.let { action ->
        val launcher = rememberLauncherForActivityResult(action.activityContract) { uri ->
            if (uri == null) {
                vm.clearSelectionAction()
            } else {
                vm.executeSelectionAction(uri)
            }
        }

        if (vm.selectedBundle == null) {
            BundleSelector(
                sources = patchBundles,
                title = action.bundleSelectorTitle
            ) {
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

    vm.resetDialogState?.let { dialogState ->
        ConfirmDialog(
            onDismiss = { vm.resetDialogState = null },
            onConfirm = dialogState.onConfirm,
            title = stringResource(dialogState.titleResId),
            description = dialogState.dialogOptionName?.let {
                stringResource(dialogState.descriptionResId, it)
            } ?: stringResource(dialogState.descriptionResId),
            icon = Icons.Outlined.WarningAmber
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
                title = stringResource(R.string.keystore),
                leadingContent = { Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
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
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ImpoxportDetailColumn(
                                title = stringResource(R.string.import_keystore_dialog_alias_field),
                                value = keystoreAlias,
                                leadingContent = {
                                    IconButton(
                                        onClick = {
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    resources.getString(R.string.import_keystore_dialog_alias_field),
                                                    keystoreAlias
                                                )
                                            )
                                            context.toast(resources.getString(R.string.toast_copied_to_clipboard))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = stringResource(R.string.copy_to_clipboard)
                                        )
                                    }
                                }
                            )
                            ImpoxportDetailColumn(
                                title = stringResource(R.string.import_keystore_dialog_password_field),
                                value = if (showKeystorePassword) keystorePass else "•".repeat(keystorePass.length),
                                leadingContent = {
                                    val hidePassword = showKeystorePassword
                                    IconButton(onClick = { showKeystorePassword = !showKeystorePassword }) {
                                        Icon(
                                            imageVector = if (hidePassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = if (hidePassword) {
                                                stringResource(R.string.hide_password_field)
                                            } else {
                                                stringResource(R.string.show_password_field)
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    resources.getString(R.string.import_keystore_dialog_password_field),
                                                    keystorePass
                                                )
                                            )
                                            context.toast(resources.getString(R.string.toast_copied_to_clipboard))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = stringResource(R.string.copy_to_clipboard)
                                        )
                                    }
                                }
                            )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = animateColorAsState(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.motionScheme.defaultEffectsSpec(),
                        "surfaceContainerLow"
                    ).value,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { importKeystoreLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.import_))
                        }
                        FilledTonalButton(
                            onClick = {
                                if (!vm.canExport()) {
                                    context.toast(resources.getString(R.string.export_keystore_unavailable))
                                    return@FilledTonalButton
                                }
                                exportKeystoreLauncher.launch("Manager.keystore")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.export))
                        }
                    }
                }
            }

            ListSection(
                title = stringResource(R.string.patches_selections),
                leadingContent = { Icon(Icons.Outlined.Source, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = animateColorAsState(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.motionScheme.defaultEffectsSpec(),
                        "surfaceContainerLow"
                    ).value,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ImpoxportDetailColumn(
                            title = stringResource(R.string.patch_selection_packages),
                            value = patchStorageStats.selectionPackageCount.toString()
                        )
                        ImpoxportDetailColumn(
                            title = stringResource(R.string.patch_selection_entries),
                            value = patchStorageStats.selectedPatchCount.toString()
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = animateColorAsState(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.motionScheme.defaultEffectsSpec(),
                        "surfaceContainerLow"
                    ).value,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = vm::importSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.import_))
                        }
                        FilledTonalButton(
                            onClick = vm::exportSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.export))
                        }
                    }
                }
            }
            ListSection(
                title = stringResource(R.string.reset),
                leadingContent = { Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                SettingsListItem(
                    onClick = {
                        vm.resetDialogState = ResetDialogState.Keystore {
                            vm.regenerateKeystore()
                        }
                    },
                    headlineContent = stringResource(R.string.regenerate_keystore),
                    supportingContent = stringResource(R.string.regenerate_keystore_description)
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
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.reset))
                }
            }
            }

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

@Composable
private fun ImpoxportDetailColumn(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    leadingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1500,
                    initialDelayMillis = 2500,
                    spacing = MarqueeSpacing.fractionOfContainer(1f / 5f),
                    velocity = 55.dp,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }

        leadingContent?.let { content ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
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