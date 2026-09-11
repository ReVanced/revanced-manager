package app.revanced.manager.ui.component.sources

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.BIN_MIMETYPE
import app.revanced.manager.util.transparentListItemColors
import kotlinx.coroutines.launch

enum class ImportSourceDialogStrings(
    val title: Int,
    val urlLabel: Int,
    val mimeType: String
) {
    PATCHES(R.string.add_patches, R.string.patches_url, BIN_MIMETYPE),
    DOWNLOADERS(R.string.downloader_add, R.string.downloader_url, APK_MIMETYPE),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportSourceDialog(
    strings: ImportSourceDialogStrings,
    onDismiss: () -> Unit,
    validateUrl: suspend (String) -> String?,
    onUrlSubmit: (String, Boolean) -> Unit,
    onFileSubmit: (Uri) -> Unit,
    methods: List<ImportMethod> = importMethods
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val method = methods[selected]

    val state = rememberSaveable(saver = ImportInputState.Saver) { ImportInputState() }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val submission = method.submissionOf(state, autoUpdate)

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
                            methods.forEachIndexed { index, entry ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(entry.label)) },
                                    onClick = {
                                        expanded = false
                                        selected = index
                                        validationError = null
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                method.Input(strings, state, validationError)

                if (method.offersAutoUpdate) {
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = submission != null && !isSubmitting,
                onClick = {
                    when (val it = submission ?: return@TextButton) {
                        is ImportSubmission.File -> onFileSubmit(it.uri)

                        is ImportSubmission.Url -> coroutineScope.launch {
                            isSubmitting = true
                            val error = validateUrl(it.url)
                            isSubmitting = false

                            if (error == null) onUrlSubmit(it.url, it.autoUpdate)
                            else validationError = error
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
