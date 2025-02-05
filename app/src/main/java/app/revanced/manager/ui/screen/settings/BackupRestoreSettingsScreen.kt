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
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.PasswordField
import app.revanced.manager.ui.component.bundle.BundleSelector
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: ImportExportViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val importKeystoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let { uri -> viewModel.startKeystoreImport(uri) }
        }
    val exportKeystoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            it?.let(viewModel::exportKeystore)
        }

    val patchBundles by viewModel.patchBundles.collectAsStateWithLifecycle(initialValue = emptyList())
    val packagesWithOptions by viewModel.packagesWithOptions.collectAsStateWithLifecycle(initialValue = emptySet())

    viewModel.selectionAction?.let { action ->
        val launcher = rememberLauncherForActivityResult(action.activityContract) { uri ->
            if (uri == null) {
                viewModel.clearSelectionAction()
            } else {
                viewModel.executeSelectionAction(uri)
            }
        }

        if (viewModel.selectedBundle == null) {
            BundleSelector(patchBundles) {
                if (it == null) {
                    viewModel.clearSelectionAction()
                } else {
                    viewModel.selectBundle(it)
                    launcher.launch(action.activityArg)
                }
            }
        }
    }

    if (viewModel.showCredentialsDialog) {
        KeystoreCredentialsDialog(
            onDismissRequest = viewModel::cancelKeystoreImport,
            onSubmit = { cn, pass ->
                viewModel.viewModelScope.launch {
                    uiSafe(context, R.string.failed_to_import_keystore, "Failed to import keystore") {
                        val result = viewModel.tryKeystoreImport(cn, pass)
                        if (!result) context.toast(context.getString(R.string.restore_keystore_wrong_credentials))
                    }
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.backup_restore),
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
            var showPackageSelector by rememberSaveable {
                mutableStateOf(false)
            }
            var showBundleSelector by rememberSaveable {
                mutableStateOf(false)
            }

            if (showPackageSelector) {
                PackageSelector(packages = packagesWithOptions) { selected ->
                    selected?.let(viewModel::resetOptionsForPackage)

                    showPackageSelector = false
                }
            }

            if (showBundleSelector) {
                BundleSelector(bundles = patchBundles) { bundle ->
                    bundle?.let(viewModel::clearOptionsForBundle)

                    showBundleSelector = false
                }
            }

            GroupHeader(stringResource(R.string.keystore))
            GroupItem(
                onClick = {
                    if (!viewModel.canExport()) {
                        context.toast(context.getString(R.string.backup_keystore_unavailable))
                        return@GroupItem
                    }
                    exportKeystoreLauncher.launch("Manager.keystore")
                },
                headline = R.string.backup,
                description = R.string.backup_keystore_description
            )
            GroupItem(
                onClick = {
                    importKeystoreLauncher.launch("*/*")
                },
                headline = R.string.restore,
                description = R.string.restore_keystore_description
            )
            GroupItem(
                onClick = viewModel::regenerateKeystore,
                headline = {
                    Text(
                        stringResource(R.string.regenerate_keystore),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                description = R.string.regenerate_keystore_description
            )

            GroupHeader(stringResource(R.string.patch_selection))
            GroupItem(
                onClick = viewModel::exportSelection,
                headline = R.string.backup,
                description = R.string.restore_patch_selection_description
            )
            GroupItem(
                onClick = viewModel::importSelection,
                headline = R.string.restore,
                description = R.string.backup_patch_selection_description
            )
            GroupItem(
                onClick = viewModel::resetSelection, // TODO: allow resetting selection for specific bundle or package name.
                headline = {
                    Text(
                        stringResource(R.string.reset),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                description = R.string.reset_patch_selection_description
            )

            GroupHeader(stringResource(R.string.patch_options))
            // TODO: patch options import/export.
            GroupItem(
                onClick = viewModel::resetOptions,
                headline = {
                    Text(
                        stringResource(R.string.reset),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                description = R.string.patch_options_reset_all_description,
            )
            GroupItem(
                onClick = { showPackageSelector = true },
                headline = {
                    Text(
                        stringResource(R.string.patch_options_reset_package),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                description = R.string.patch_options_reset_package_description
            )
            if (patchBundles.size > 1) {
                GroupItem(
                    onClick = { showBundleSelector = true },
                    headline = {
                        Text(
                            stringResource(R.string.patch_options_reset_bundle),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    description = R.string.patch_options_reset_bundle_description,
                )
            }
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
private fun GroupItem(
    onClick: () -> Unit,
    headline: @Composable () -> Unit,
    @StringRes description: Int? = null
) {
    SettingsListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = headline,
        supportingContent = description?.let { stringResource(it) }
    )
}

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
                Text(stringResource(R.string.restore_keystore_dialog_button))
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
                text = stringResource(R.string.restore_keystore_dialog_title),
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
                    text = stringResource(R.string.restore_keystore_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cn,
                    onValueChange = { cn = it },
                    label = { Text(stringResource(R.string.restore_keystore_dialog_alias_field)) }
                )
                PasswordField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(stringResource(R.string.restore_keystore_dialog_password_field)) }
                )
            }
        }
    )
}