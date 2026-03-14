package app.revanced.manager.ui.component.bundle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.TextHorizontalPadding
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.util.BIN_MIMETYPE
import app.revanced.manager.util.transparentListItemColors

private enum class SourceType {
    Local,
    Remote
}

enum class ImportSourceDialogStrings(
    val title: Int,
    val type_description: Int,
    val type_remote_description: Int,
    val type_local_description: Int,
    val import_local: Int,
    val import_remote: Int
) {
    PATCHES(
        R.string.add_patches,
        R.string.select_patches_type_dialog_description,
        R.string.remote_patches_description,
        R.string.local_patches_description,
        R.string.patches,
        R.string.patches_url
    ),
    DOWNLOADERS(
        R.string.downloader_add,
        R.string.select_downloader_type_dialog_description,
        R.string.remote_downloaders_description,
        R.string.local_downloaders_description,
        R.string.downloaders,
        R.string.downloader_url
    ),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportSourceDialog(
    strings: ImportSourceDialogStrings,
    onDismiss: () -> Unit,
    onRemoteSubmit: (String, Boolean) -> Unit,
    onLocalSubmit: (Uri) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var sourceType by rememberSaveable { mutableStateOf(SourceType.Remote) }
    var local by rememberSaveable { mutableStateOf<Uri?>(null) }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }

    val fileActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { local = it }
        }

    fun launchFileActivity() {
        fileActivityLauncher.launch(BIN_MIMETYPE)
    }

    val steps = listOf<@Composable () -> Unit>(
        {
            SelectSourceTypeStep(strings, sourceType) { selectedType ->
                sourceType = selectedType
            }
        },
        {
            ImportSourceStep(
                strings,
                sourceType,
                local,
                remoteUrl,
                autoUpdate,
                ::launchFileActivity,
                { remoteUrl = it },
                { autoUpdate = it }
            )
        }
    )

    val inputsAreValid by remember {
        derivedStateOf {
            (sourceType == SourceType.Local && local != null) ||
                    (sourceType == SourceType.Remote && remoteUrl.isNotEmpty())
        }
    }

    AlertDialogExtended(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (currentStep == 0) R.string.select else strings.title))
        },
        text = {
            steps[currentStep]()
        },
        confirmButton = {
            if (currentStep == steps.lastIndex) {
                TextButton(
                    enabled = inputsAreValid,
                    onClick = {
                        when (sourceType) {
                            SourceType.Local -> local?.let(onLocalSubmit)
                            SourceType.Remote -> onRemoteSubmit(remoteUrl, autoUpdate)
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.add))
                }
            } else {
                TextButton(onClick = { currentStep++ }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.next))
                }
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.back))
                }
            } else {
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}

@Composable
private fun SelectSourceTypeStep(
    strings: ImportSourceDialogStrings,
    sourceType: SourceType,
    onSourceTypeSelected: (SourceType) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(strings.type_description)
        )
        Column {
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onSourceTypeSelected(SourceType.Remote) }
                ),
                headlineContent = { Text(stringResource(R.string.enter_url)) },
                overlineContent = { Text(stringResource(R.string.recommended)) },
                supportingContent = { Text(stringResource(strings.type_remote_description)) },
                leadingContent = {
                    HapticRadioButton(
                        selected = sourceType == SourceType.Remote,
                        onClick = null
                    )
                },
                colors = transparentListItemColors
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onSourceTypeSelected(SourceType.Local) }
                ),
                headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                supportingContent = { Text(stringResource(strings.type_local_description)) },
                overlineContent = { },
                leadingContent = {
                    HapticRadioButton(
                        selected = sourceType == SourceType.Local,
                        onClick = null
                    )
                },
                colors = transparentListItemColors
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportSourceStep(
    strings: ImportSourceDialogStrings,
    sourceType: SourceType,
    local: Uri?,
    remoteUrl: String,
    autoUpdate: Boolean,
    launchFileActivity: () -> Unit,
    onRemoteUrlChange: (String) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        when (sourceType) {
            SourceType.Local -> {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(strings.import_local))
                        },
                        supportingContent = { Text(stringResource(if (local != null) R.string.file_field_set else R.string.file_field_not_set)) },
                        trailingContent = {
                            IconButton(
                                onClick = launchFileActivity,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(imageVector = Icons.Default.Topic, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { launchFileActivity() },
                        colors = transparentListItemColors
                    )
                }
            }

            SourceType.Remote -> {
                Column(
                    modifier = Modifier.padding(TextHorizontalPadding)
                ) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = onRemoteUrlChange,
                        label = { Text(stringResource(strings.import_remote)) }
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    ListItem(
                        modifier = Modifier.clickable(
                            role = Role.Checkbox,
                            onClick = { onAutoUpdateChange(!autoUpdate) }
                        ),
                        headlineContent = { Text(stringResource(R.string.auto_update)) },
                        leadingContent = {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                HapticCheckbox(
                                    checked = autoUpdate,
                                    onCheckedChange = {
                                        onAutoUpdateChange(!autoUpdate)
                                    }
                                )
                            }
                        },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    }
}