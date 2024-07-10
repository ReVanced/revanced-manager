package app.revanced.manager.ui.component.bundle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.TextHorizontalPadding
import app.revanced.manager.ui.model.BundleType
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.JAR_MIMETYPE

@Composable
fun ImportPatchBundleDialog(
    onDismiss: () -> Unit,
    onRemoteSubmit: (String, Boolean) -> Unit,
    onLocalSubmit: (Uri, Uri?) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var bundleType by rememberSaveable { mutableStateOf(BundleType.Remote) }
    var patchBundle by rememberSaveable { mutableStateOf<Uri?>(null) }
    var integrations by rememberSaveable { mutableStateOf<Uri?>(null) }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var autoUpdate by rememberSaveable { mutableStateOf(false) }

    val patchActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { patchBundle = it }
        }

    fun launchPatchActivity() {
        patchActivityLauncher.launch(JAR_MIMETYPE)
    }

    val integrationsActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { integrations = it }
        }

    fun launchIntegrationsActivity() {
        integrationsActivityLauncher.launch(APK_MIMETYPE)
    }

    val steps = listOf<@Composable () -> Unit>(
        {
            SelectBundleTypeStep(bundleType) { selectedType ->
                bundleType = selectedType
            }
        },
        {
            ImportBundleStep(
                bundleType,
                patchBundle,
                integrations,
                remoteUrl,
                autoUpdate,
                { launchPatchActivity() },
                { launchIntegrationsActivity() },
                { remoteUrl = it },
                { autoUpdate = it }
            )
        }
    )

    val inputsAreValid by remember {
        derivedStateOf {
            (bundleType == BundleType.Local && patchBundle != null) ||
                    (bundleType == BundleType.Remote && remoteUrl.isNotEmpty())
        }
    }

    AlertDialogExtended(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (currentStep == 0) R.string.select else R.string.add_patch_bundle))
        },
        text = {
            steps[currentStep]()
        },
        confirmButton = {
            if (currentStep == steps.lastIndex) {
                TextButton(
                    enabled = inputsAreValid,
                    onClick = {
                        when (bundleType) {
                            BundleType.Local -> patchBundle?.let {
                                onLocalSubmit(
                                    it,
                                    integrations
                                )
                            }

                            BundleType.Remote -> onRemoteSubmit(remoteUrl, autoUpdate)
                        }
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            } else {
                TextButton(onClick = { currentStep++ }) {
                    Text(stringResource(R.string.next))
                }
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text(stringResource(R.string.back))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}

@Composable
fun SelectBundleTypeStep(
    bundleType: BundleType,
    onBundleTypeSelected: (BundleType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.select_bundle_type_dialog_description)
        )
        Column {
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onBundleTypeSelected(BundleType.Remote) }
                ),
                headlineContent = { Text(stringResource(R.string.enter_url)) },
                overlineContent = { Text(stringResource(R.string.recommended)) },
                supportingContent = { Text(stringResource(R.string.remote_bundle_description)) },
                leadingContent = {
                    RadioButton(
                        selected = bundleType == BundleType.Remote,
                        onClick = null
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onBundleTypeSelected(BundleType.Local) }
                ),
                headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                supportingContent = { Text(stringResource(R.string.local_bundle_description)) },
                overlineContent = { },
                leadingContent = {
                    RadioButton(
                        selected = bundleType == BundleType.Local,
                        onClick = null
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBundleStep(
    bundleType: BundleType,
    patchBundle: Uri?,
    integrations: Uri?,
    remoteUrl: String,
    autoUpdate: Boolean,
    launchPatchActivity: () -> Unit,
    launchIntegrationsActivity: () -> Unit,
    onRemoteUrlChange: (String) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit
) {
    Column {
        when (bundleType) {
            BundleType.Local -> {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.patch_bundle_field))
                        },
                        supportingContent = { Text(stringResource(if (patchBundle != null) R.string.file_field_set else R.string.file_field_not_set)) },
                        trailingContent = {
                            IconButton(onClick = launchPatchActivity) {
                                Icon(imageVector = Icons.Default.Topic, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { launchPatchActivity() }
                    )
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.integrations_field))
                        },
                        supportingContent = { Text(stringResource(if (integrations != null) R.string.file_field_set else R.string.file_field_not_set)) },
                        trailingContent = {
                            IconButton(onClick = launchIntegrationsActivity) {
                                Icon(imageVector = Icons.Default.Topic, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { launchIntegrationsActivity() }
                    )
                }
            }

            BundleType.Remote -> {
                Column(
                    modifier = Modifier.padding(TextHorizontalPadding)
                ) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = onRemoteUrlChange,
                        label = { Text(stringResource(R.string.bundle_url)) }
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    ListItem(
                        modifier = Modifier.clickable(
                            role = Role.Checkbox,
                            onClick = { onAutoUpdateChange(!autoUpdate) }
                        ),
                        headlineContent = { Text(stringResource(R.string.auto_update)) },
                        leadingContent = {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Checkbox(
                                    checked = autoUpdate,
                                    onCheckedChange = {
                                        onAutoUpdateChange(!autoUpdate)
                                    }
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}