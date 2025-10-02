package app.revanced.manager.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.PasswordField
import app.revanced.manager.ui.component.bundle.BundleSelector
import app.revanced.manager.ui.component.settings.ExpandableSettingListItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.viewmodel.ResetDialogState
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSettingsScreen(
    onBackClick: () -> Unit,
    vm: ImportExportViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectorDialog by rememberSaveable { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val importKeystoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let { uri -> vm.startKeystoreImport(uri) }
        }
    val exportKeystoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            it?.let(vm::exportKeystore)
        }

    val patchBundles by vm.patchBundles.collectAsStateWithLifecycle(initialValue = emptyList())
    val packagesWithSelections by vm.packagesWithSelection.collectAsStateWithLifecycle(initialValue = emptySet())
    val packagesWithOptions by vm.packagesWithOptions.collectAsStateWithLifecycle(initialValue = emptySet())

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
                        if (!result) context.toast(context.getString(R.string.import_keystore_wrong_credentials))
                    }
                }
            }
        )
    }

    vm.resetDialogState?.let {
        with(vm.resetDialogState!!) {
            ConfirmDialog(
                onDismiss = { vm.resetDialogState = null },
                onConfirm = onConfirm,
                title = stringResource(titleResId),
                description = dialogOptionName?.let {
                    stringResource(descriptionResId, it)
                } ?: stringResource(descriptionResId),
                icon = Icons.Outlined.WarningAmber
            )
        }
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
            selectorDialog?.invoke()

            GroupHeader(stringResource(R.string.import_))
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

            GroupHeader(stringResource(R.string.export))
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
                onClick = vm::exportSelection,
                headline = R.string.export_patch_selection,
                description = R.string.export_patch_selection_description
            )

            GroupHeader(stringResource(R.string.reset))
            GroupItem(
                onClick = {
                    vm.resetDialogState = ResetDialogState.Keystore {
                        vm.regenerateKeystore()
                    }
                },
                headline = R.string.regenerate_keystore,
                description = R.string.regenerate_keystore_description
            )

            ExpandableSettingListItem(
                headlineContent = stringResource(R.string.reset_patch_selection),
                supportingContent = stringResource(R.string.reset_patch_selection_description),
                expandableContent = {
                    GroupItem(
                        onClick = {
                            vm.resetDialogState = ResetDialogState.PatchSelectionAll {
                                vm.resetSelection()
                            }
                        },
                        headline = R.string.patch_selection_reset_all,
                        description = R.string.patch_selection_reset_all_description
                    )

                    GroupItem(
                        onClick = {
                            selectorDialog = {
                                PackageSelector(packages = packagesWithSelections) { packageName ->
                                    packageName?.also {
                                        vm.resetDialogState =
                                            ResetDialogState.PatchSelectionPackage(packageName) {
                                                vm.resetSelectionForPackage(packageName)
                                            }
                                    }
                                    selectorDialog = null
                                }
                            }
                        },
                        headline = R.string.patch_selection_reset_package,
                        description = R.string.patch_selection_reset_package_description
                    )

                    if (patchBundles.isNotEmpty()) {
                        GroupItem(
                            onClick = {
                                selectorDialog = {
                                    BundleSelector(sources = patchBundles) { src ->
                                        src?.also {
                                            coroutineScope.launch {
                                                vm.resetDialogState =
                                                    ResetDialogState.PatchSelectionBundle(it.name) {
                                                        vm.resetSelectionForPatchBundle(it)
                                                    }
                                            }
                                        }
                                        selectorDialog = null
                                    }
                                }
                            },
                            headline = R.string.patch_selection_reset_patches,
                            description = R.string.patch_selection_reset_patches_description
                        )
                    }
                }
            )

            ExpandableSettingListItem(
                headlineContent = stringResource(R.string.reset_patch_options),
                supportingContent = stringResource(R.string.reset_patch_options_description),
                expandableContent = {
                    GroupItem(
                        onClick = {
                            vm.resetDialogState = ResetDialogState.PatchOptionsAll {
                                vm.resetOptions()
                            }
                        }, // TODO: patch options import/export.
                        headline = R.string.patch_options_reset_all,
                        description = R.string.patch_options_reset_all_description,
                    )

                    GroupItem(
                        onClick = {
                            selectorDialog = {
                                PackageSelector(packages = packagesWithOptions) { packageName ->
                                    packageName?.also {
                                        vm.resetDialogState =
                                            ResetDialogState.PatchOptionPackage(packageName) {
                                                vm.resetOptionsForPackage(packageName)
                                            }
                                    }
                                    selectorDialog = null
                                }
                            }
                        },
                        headline = R.string.patch_options_reset_package,
                        description = R.string.patch_options_reset_package_description
                    )

                    if (patchBundles.isNotEmpty()) {
                        GroupItem(
                            onClick = {
                                selectorDialog = {
                                    BundleSelector(sources = patchBundles) { src ->
                                        src?.also {
                                            coroutineScope.launch {
                                                vm.resetDialogState =
                                                    ResetDialogState.PatchOptionBundle(src.name) {
                                                        vm.resetOptionsForBundle(src)
                                                    }
                                            }
                                        }
                                        selectorDialog = null
                                    }
                                }
                            },
                            headline = R.string.patch_options_reset_patches,
                            description = R.string.patch_options_reset_patches_description,
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageSelector(packages: Set<String>, onFinish: (String?) -> Unit) {
    val context = LocalContext.current

    val noPackages = packages.isEmpty()

    LaunchedEffect(noPackages) {
        if (noPackages) {
            context.toast("No packages available.")
            onFinish(null)
        }
    }

    if (noPackages) return

    ModalBottomSheet(
        onDismissRequest = { onFinish(null) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select package",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            packages.forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .clickable {
                            onFinish(it)
                        }
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
        modifier = Modifier.clickable { onClick() },
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