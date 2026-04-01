package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.TooltipIconButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptionsDialog(
    patch: PatchInfo,
    values: Map<String, Any?>?,
    reset: () -> Unit,
    resetOption: (Option<*>) -> Unit,
    set: (String, Any?) -> Unit,
    onDismissRequest: () -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean
) {
    val invalidOptions = remember { mutableStateMapOf<Option<*>, Boolean>() }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DiscardInvalidDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onDismissRequest()
            }
        )
    }

    val onBackClick = {
        if (invalidOptions.values.any { it }) {
            showDialog = true
        } else {
            onDismissRequest()
        }
    }

    FullscreenDialog(onDismissRequest = onBackClick) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = patch.name,
                    onBackClick = onBackClick,
                    actions = {
                        if (!readOnly) {
                            TooltipIconButton(
                                onClick = reset,
                                tooltip = stringResource(R.string.reset)
                            ) {
                                Icon(Icons.Filled.Restore, stringResource(R.string.reset))
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumnWithScrollbar(
                modifier = Modifier.padding(paddingValues)
            ) {
                patch.options ?: return@LazyColumnWithScrollbar

                items(patch.options, key = { it.name }) { option ->
                    val name = option.name
                    val usingDefault = values == null || name !in values
                    val value = if (usingDefault) option.default else values[name]

                    @Suppress("UNCHECKED_CAST")
                    OptionItem(
                        option = option as Option<Any>,
                        value = value,
                        setValue = { set(name, it) },
                        isDefault = usingDefault,
                        reset = { resetOption(option) },
                        selectionWarningEnabled = selectionWarningEnabled,
                        readOnly = readOnly,
                        setInvalid = { invalidOptions[option] = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscardInvalidDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.discard_changes_dialog_title)) },
        text = { Text(stringResource(R.string.patch_options_discard_invalid_description)) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.discard_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}