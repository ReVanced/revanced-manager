package app.revanced.manager.ui.component.sources

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.webkit.URLUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.TextHorizontalPadding
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.BIN_MIMETYPE
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.launch

private enum class ImportMethod(val label: Int) {
    // Free text: any protocol, the scheme decides how the source is resolved.
    Auto(R.string.import_source_method_auto),

    // A web address, the most common kind of source.
    Http(R.string.import_source_method_http),

    // A local file, picked through the system file picker.
    File(R.string.import_source_method_file)
}

enum class ImportSourceDialogStrings(
    val title: Int,
    val urlLabel: Int
) {
    PATCHES(R.string.add_patches, R.string.patches_url),
    DOWNLOADERS(R.string.downloader_add, R.string.downloader_url),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportSourceDialog(
    strings: ImportSourceDialogStrings,
    onDismiss: () -> Unit,
    validateUrl: suspend (String) -> String?,
    onUrlSubmit: (String, Boolean) -> Unit,
    onFileSubmit: (Uri) -> Unit
) {
    var method by rememberSaveable { mutableStateOf(ImportMethod.Auto) }
    var url by rememberSaveable { mutableStateOf("") }
    var file by rememberSaveable { mutableStateOf<Uri?>(null) }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }
    var urlValidationError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val fileActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
            picked?.let {
                file = it
                // Picking a file continues in the file flow no matter which method started it.
                method = ImportMethod.File
            }
        }

    fun launchFileActivity() = fileActivityLauncher.launch(
        when (strings) {
            ImportSourceDialogStrings.PATCHES -> BIN_MIMETYPE
            ImportSourceDialogStrings.DOWNLOADERS -> APK_MIMETYPE
        }
    )

    // Auto accepts any text and defers the scheme to the handlers, URL expects a
    // well-formed web address before the network is even touched.
    val isValidUrl = url.trim().let { URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) }
    val inputsAreValid by remember {
        derivedStateOf {
            when (method) {
                ImportMethod.Auto -> url.isNotBlank()
                ImportMethod.Http -> isValidUrl
                ImportMethod.File -> file != null
            }
        }
    }

    AlertDialogExtended(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(strings.title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.padding(TextHorizontalPadding)) {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = stringResource(method.label),
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.import_source_method)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ImportMethod.entries.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(entry.label)) },
                                    onClick = {
                                        expanded = false
                                        method = entry
                                        urlValidationError = null
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (method) {
                    ImportMethod.Auto, ImportMethod.Http -> {
                        val showValidator = method == ImportMethod.Http && url.isNotEmpty() && !isValidUrl
                        Column(modifier = Modifier.padding(TextHorizontalPadding)) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = {
                                    url = it
                                    urlValidationError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    autoCorrectEnabled = false
                                ),
                                singleLine = true,
                                label = {
                                    Text(
                                        stringResource(
                                            if (method == ImportMethod.Auto) R.string.import_source_input
                                            else strings.urlLabel
                                        )
                                    )
                                },
                                placeholder = if (method == ImportMethod.Http) {
                                    { Text("https://") }
                                } else null,
                                // Auto accepts a file too so offer the picker without making the user switch methods first.
                                trailingIcon = if (method == ImportMethod.Auto) {
                                    {
                                        TooltipIconButton(
                                            onClick = ::launchFileActivity,
                                            tooltip = stringResource(R.string.select_from_storage)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                } else null,
                                isError = showValidator || urlValidationError != null,
                                supportingText = {
                                    when {
                                        urlValidationError != null -> Text(urlValidationError!!)
                                        showValidator -> Text(stringResource(R.string.input_dialog_value_invalid))
                                    }
                                }
                            )
                        }
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            ListItem(
                                modifier = Modifier.clickable(
                                    role = Role.Checkbox,
                                    onClick = { autoUpdate = !autoUpdate }
                                ),
                                headlineContent = { Text(stringResource(R.string.auto_update)) },
                                leadingContent = {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        HapticCheckbox(
                                            checked = autoUpdate,
                                            onCheckedChange = { autoUpdate = !autoUpdate }
                                        )
                                    }
                                },
                                colors = transparentListItemColors
                            )
                        }
                    }

                    ImportMethod.File -> FilePickerCard(
                        file = file,
                        onClick = ::launchFileActivity
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = inputsAreValid && !isSubmitting,
                onClick = {
                    when (method) {
                        ImportMethod.File -> file?.let(onFileSubmit)
                        ImportMethod.Auto, ImportMethod.Http -> {
                            val trimmedUrl = url.trim()
                            coroutineScope.launch {
                                isSubmitting = true
                                val validationError = validateUrl(trimmedUrl)
                                isSubmitting = false

                                if (validationError == null) onUrlSubmit(trimmedUrl, autoUpdate)
                                else urlValidationError = validationError
                            }
                        }
                    }
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}

@Composable
private fun FilePickerCard(
    file: Uri?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val info = remember(file) { file?.let { context.readFileInfo(it) } }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = {
                Text(info?.name ?: stringResource(R.string.file_field_not_set))
            },
            supportingContent = {
                Text(
                    info?.size?.let { Formatter.formatShortFileSize(context, it) }
                        ?: stringResource(R.string.import_source_press_to_select)
                )
            },
            leadingContent = if (file != null) {
                {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else null,
            trailingContent = {
                TooltipIconButton(
                    onClick = onClick,
                    tooltip = stringResource(R.string.select_from_storage)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                }
            },
            colors = transparentListItemColors
        )
    }
}

private class FileInfo(val name: String?, val size: Long?)

private fun Context.readFileInfo(uri: Uri): FileInfo = runCatching {
    contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null

        fun column(name: String) = cursor.getColumnIndex(name).takeIf { it >= 0 && !cursor.isNull(it) }

        FileInfo(
            name = column(OpenableColumns.DISPLAY_NAME)?.let(cursor::getString),
            size = column(OpenableColumns.SIZE)?.let(cursor::getLong)
        )
    }
}.getOrNull() ?: FileInfo(uri.lastPathSegment, null)
