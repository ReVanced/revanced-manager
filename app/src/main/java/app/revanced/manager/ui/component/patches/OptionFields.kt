package app.revanced.manager.ui.component.patches

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.data.platform.FileSystem
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.util.toast
import app.revanced.patcher.patch.options.types.*
import org.koin.compose.rememberKoinInject

// Composable functions do not support function references, so we have to use composable lambdas instead.
private typealias OptionImpl = @Composable (Option, Any?, (Any?) -> Unit) -> Unit

@Composable
private fun OptionListItem(
    option: Option,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(option.title) },
        supportingContent = { Text(option.description) },
        trailingContent = trailingContent
    )
}

@Composable
private fun StringOptionDialog(
    name: String,
    value: String?,
    onSubmit: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showFileDialog by rememberSaveable { mutableStateOf(false) }
    var fieldValue by rememberSaveable(value) {
        mutableStateOf(value.orEmpty())
    }

    val fs: FileSystem = rememberKoinInject()
    val (contract, permissionName) = fs.permissionContract()
    val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
        showFileDialog = it
    }

    if (showFileDialog) {
        PathSelectorDialog(
            root = fs.externalFilesDir()
        ) {
            showFileDialog = false
            it?.let { path ->
                fieldValue = path.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(name) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                placeholder = {
                    Text(stringResource(R.string.string_option_placeholder))
                },
                trailingIcon = {
                    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
                    IconButton(
                        onClick = { showDropdownMenu = true }
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.string_option_menu_description)
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Outlined.Folder, null)
                            },
                            text = {
                                Text(stringResource(R.string.path_selector))
                            },
                            onClick = {
                                showDropdownMenu = false
                                if (fs.hasStoragePermission()) {
                                    showFileDialog = true
                                } else {
                                    permissionLauncher.launch(permissionName)
                                }
                            }
                        )
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(fieldValue) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private val StringOption: OptionImpl = { option, value, setValue ->
    var showInputDialog by rememberSaveable { mutableStateOf(false) }
    fun showInputDialog() {
        showInputDialog = true
    }

    fun dismissInputDialog() {
        showInputDialog = false
    }

    if (showInputDialog) {
        StringOptionDialog(
            name = option.title,
            value = value as? String,
            onSubmit = {
                dismissInputDialog()
                setValue(it)
            },
            onDismissRequest = ::dismissInputDialog
        )
    }

    OptionListItem(
        option = option,
        onClick = ::showInputDialog
    ) {
        IconButton(onClick = ::showInputDialog) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.string_option_icon_description)
            )
        }
    }
}

private val BooleanOption: OptionImpl = { option, value, setValue ->
    val current = (value as? Boolean) ?: false

    OptionListItem(
        option = option,
        onClick = { setValue(!current) }
    ) {
        Switch(checked = current, onCheckedChange = setValue)
    }
}

private val UnknownOption: OptionImpl = { option, _, _ ->
    val context = LocalContext.current
    OptionListItem(
        option = option,
        onClick = { context.toast("Unknown type: ${option.type.name}") },
        trailingContent = {})
}

@Composable
fun OptionItem(option: Option, value: Any?, setValue: (Any?) -> Unit) {
    val implementation = remember(option.type) {
        when (option.type) {
            // These are the only two types that are currently used by the official patches.
            StringPatchOption::class.java -> StringOption
            BooleanPatchOption::class.java -> BooleanOption
            else -> UnknownOption
        }
    }

    implementation(option, value, setValue)
}