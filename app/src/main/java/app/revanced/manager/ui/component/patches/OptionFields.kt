@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.revanced.manager.ui.component.patches

import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FloatInputDialog
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.IntInputDialog
import app.revanced.manager.ui.component.LongInputDialog
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.saver.snapshotStateSetSaver
import app.revanced.manager.util.transparentListItemColors
import kotlinx.parcelize.Parcelize
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.Serializable
import kotlin.random.Random
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private fun formatValue(
    v: Any?,
    boolTrueStr: String = "true",
    boolFalseStr: String = "false",
): String = when (v) {
    null -> ""
    true -> boolTrueStr
    false -> boolFalseStr
    else -> v.toString()
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> parseValue(str: String, type: KType): T? {
    return try {
        when (type) {
            typeOf<String>() -> str as T
            typeOf<Int>() -> str.toIntOrNull() as T?
            typeOf<Long>() -> str.toLongOrNull() as T?
            typeOf<Float>() -> str.toFloatOrNull() as T?
            typeOf<Boolean>() -> str.toBooleanStrictOrNull() as T?
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun safeguardActive(
    readOnly: Boolean,
    isRequired: Boolean,
    selectionWarningEnabled: Boolean,
) = !readOnly && !isRequired && selectionWarningEnabled

@Composable
private fun OptionItemHeadline(option: Option<*>) {
    Text(
        buildAnnotatedString {
            append(option.name)
            if (option.required) {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
                    append(" *")
                }
            }
        }
    )
}

@Composable
private fun OptionItemCheckbox(
    checked: Boolean,
    isRequired: Boolean,
    safeguardActive: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSafeguardClick: () -> Unit,
) {
    Box {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (!safeguardActive) onCheckedChange(it) },
            enabled = !isRequired,
        )
        if (safeguardActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onSafeguardClick)
            )
        }
    }
}

@Composable
fun <T : Any> OptionItem(
    option: Option<T>,
    value: T?,
    isDefault: Boolean,
    setValue: (T?) -> Unit,
    reset: () -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean = false,
) {
    val isRequired = option.required
    val type = option.type

    if (type.classifier == List::class) {
        @Suppress("UNCHECKED_CAST")
        ListOptionItem(
            option = option as Option<List<Serializable>>,
            value = value as List<Serializable>?,
            setValue = setValue as (List<Serializable>?) -> Unit,
            selectionWarningEnabled = selectionWarningEnabled,
            readOnly = readOnly,
        )
        return
    }

    val nullValueString = stringResource(R.string.patch_options_value_null)
    val enabledBooleanValueString = stringResource(R.string.patch_options_value_boolean_true)
    val disabledBooleanValueString = stringResource(R.string.patch_options_value_boolean_false)

    val isBoolean = type == typeOf<Boolean>()
    val isString = type == typeOf<String>()

    val safeguard = safeguardActive(readOnly, isRequired, selectionWarningEnabled)

    // Actual value here
    var localValue: T? by rememberSaveable(value) { mutableStateOf(value) }

    var isChecked by rememberSaveable(value) {
        mutableStateOf(value != null || isRequired)
    }

    // For number-based/string fields, value will be parsed, validated, and set in localValue
    var localText by rememberSaveable(value) {
        mutableStateOf(value?.toString() ?: "")
    }

    val parsedValue: T? = if (!isBoolean) parseValue(localText, type) else localValue
    val isValid = parsedValue != null && option.validator(parsedValue)

    val displayText = when {
        !isChecked -> nullValueString
        isBoolean ->
            formatValue(localValue, enabledBooleanValueString, disabledBooleanValueString)

        else -> localText
    }

    var showSelectionWarningDialog by rememberSaveable { mutableStateOf(false) }
    if (showSelectionWarningDialog) {
        SelectionWarningDialog(onDismiss = { showSelectionWarningDialog = false })
    }

    val fs: Filesystem? = if (isString) koinInject() else null
    var showFileDialog by rememberSaveable { mutableStateOf(false) }

    if (isString && showFileDialog) {
        PathSelectorDialog(root = fs!!.externalFilesDir()) { path ->
            showFileDialog = false
            path?.let {
                val str = it.toString()
                localText = str
                @Suppress("UNCHECKED_CAST")
                val typed = str as T
                localValue = typed
                if (isChecked) setValue(typed)
            }
        }
    }

    val permissionLauncher = if (isString) {
        val (contract, permissionName) = fs!!.permissionContract()
        rememberLauncherForActivityResult(contract) { granted ->
            if (granted) showFileDialog = true
        } to permissionName
    } else null

    val keyboardType = when (type) {
        typeOf<Float>() -> KeyboardType.Decimal
        typeOf<Int>(), typeOf<Long>() -> KeyboardType.Number
        else -> KeyboardType.Text
    }

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = transparentListItemColors,
        headlineContent = { OptionItemHeadline(option) },
        leadingContent = if (!readOnly) {
            {
                OptionItemCheckbox(
                    checked = isChecked,
                    isRequired = isRequired,
                    safeguardActive = safeguard,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        if (checked) {
                            if (isBoolean) {
                                @Suppress("UNCHECKED_CAST")
                                val boolVal = (localValue ?: option.default ?: false) as T
                                localValue = boolVal
                                setValue(boolVal)
                            } else {
                                if (option.default != null) reset()
                                // Some options don't have defaults, so set to non-default empty text field instead
                                else {
                                    localValue = parsedValue
                                    setValue(parsedValue)
                                }
                            }
                        } else {
                            localValue = null
                            setValue(null)
                        }
                    },
                    onSafeguardClick = { showSelectionWarningDialog = true },
                )
            }
        } else null,
        supportingContent = {
            Column {
                Text(option.description)
                Spacer(Modifier.height(8.dp))

                OptionDropdownSection(
                    option = option,
                    displayText = displayText,
                    isChecked = isChecked,
                    isDefault = isDefault,
                    readOnly = readOnly,
                    safeguard = safeguard,
                    canShowError = isChecked && !isBoolean && !isValid,
                    isBoolean = isBoolean,
                    isString = isString,
                    keyboardType = keyboardType,
                    fs = fs,
                    permissionLauncher = permissionLauncher,
                    enabledBooleanLabel = enabledBooleanValueString,
                    disabledBooleanLabel = disabledBooleanValueString,
                    onValueChange = { newText ->
                        localText = newText
                        val newParsed: T? = parseValue(newText, type)
                        if (newParsed != null && option.validator(newParsed)) {
                            localValue = newParsed
                            setValue(newParsed)
                        }
                    },
                    onReset = {
                        localValue = option.default
                        localText = formatValue(option.default)
                        reset()
                    },
                    onPresetSelect = { preset ->
                        if (isBoolean) {
                            localValue = preset
                        } else {
                            localText = preset.toString()
                            localValue = preset
                        }
                        setValue(preset)
                    },
                    onFileDialogRequest = { showFileDialog = true },
                    onSafeguardClick = { showSelectionWarningDialog = true },
                )
            }
        },
    )
}

@Composable
private fun <T : Any> OptionDropdownSection(
    option: Option<T>,
    displayText: String,
    isChecked: Boolean,
    isDefault: Boolean,
    readOnly: Boolean,
    safeguard: Boolean,
    canShowError: Boolean,
    isBoolean: Boolean,
    isString: Boolean,
    keyboardType: KeyboardType,
    fs: Filesystem?,
    permissionLauncher: Pair<ActivityResultLauncher<String>, String>?,
    enabledBooleanLabel: String,
    disabledBooleanLabel: String,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
    onPresetSelect: (T) -> Unit,
    onFileDialogRequest: () -> Unit,
    onSafeguardClick: () -> Unit,
) {
    val optionsList = remember(option, enabledBooleanLabel, disabledBooleanLabel) {
        buildList {
            @Suppress("UNCHECKED_CAST")
            if (isBoolean) {
                add(enabledBooleanLabel to true as T)
                add(disabledBooleanLabel to false as T)
            }
            option.presets?.forEach { (k, v) -> if (v != null) add(k to v) }
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Box {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!safeguard && !readOnly && isChecked) expanded = it
            },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        if (!readOnly) ExposedDropdownMenuAnchorType.PrimaryEditable
                        else ExposedDropdownMenuAnchorType.PrimaryNotEditable
                    ),
                maxLines = 5,
                value = displayText,
                onValueChange = { if (!safeguard) onValueChange(it) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (readOnly && option.default == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        0.75f
                    )
                    else LocalTextStyle.current.color
                ),
                // Read-only will always appear enabled but read-only
                enabled = readOnly || isChecked,
                readOnly = readOnly || isBoolean,
                isError = canShowError,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                supportingText = when {
                    canShowError -> {
                        { Text(stringResource(R.string.patch_options_value_invalid)) }
                    }

                    isDefault && isChecked && !readOnly -> {
                        { Text(stringResource(R.string.patch_options_using_default_value)) }
                    }

                    else -> null
                },
                trailingIcon = {
                    if (!readOnly) ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                        modifier = Modifier.menuAnchor(
                            ExposedDropdownMenuAnchorType.SecondaryEditable
                        ),
                    )
                },
            )

            if (optionsList.isNotEmpty() || isString) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    if (option.default != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Filled.Restore, stringResource(R.string.reset))
                            },
                            text = { Text(stringResource(R.string.patch_options_set_default)) },
                            supportingText = {
                                Text(
                                    formatValue(
                                        option.default,
                                        enabledBooleanLabel,
                                        disabledBooleanLabel,
                                    )
                                )
                            },
                            onClick = {
                                expanded = false
                                onReset()
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    }

                    if (isString) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Folder, null) },
                            text = { Text(stringResource(R.string.path_selector)) },
                            onClick = {
                                expanded = false
                                if (fs!!.hasStoragePermission()) {
                                    onFileDialogRequest()
                                } else {
                                    permissionLauncher!!.first.launch(permissionLauncher.second)
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    }

                    optionsList.forEach { (label, presetVal) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            // Boolean fields will not have supportingText
                            supportingText = if (!isBoolean) {
                                { Text(formatValue(presetVal)) }
                            } else null,
                            onClick = {
                                expanded = false
                                onPresetSelect(presetVal)
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }
        }

        if (safeguard) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onSafeguardClick)
            )
        }
    }
}

@Composable
private fun ListOptionItemEditTrailing(readOnly: Boolean, onClick: () -> Unit) {
    TooltipIconButton(
        onClick = onClick,
        tooltip = stringResource(if (readOnly) R.string.show else R.string.edit),
    ) {
        Icon(
            if (readOnly) Icons.Outlined.Visibility else Icons.Outlined.Edit,
            stringResource(if (readOnly) R.string.show else R.string.edit),
        )
    }
}

@Composable
private fun ListOptionItem(
    option: Option<List<Serializable>>,
    value: List<Serializable>?,
    setValue: (List<Serializable>?) -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectionWarningDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectionWarningDialog) {
        SelectionWarningDialog(onDismiss = { showSelectionWarningDialog = false })
    }

    if (showDialog) {
        ListOptionDialog(
            option = option,
            readOnly = readOnly,
            value = value ?: emptyList(),
            setValue = setValue,
            onDismiss = { showDialog = false },
        )
    }

    val isRequired = option.required
    val safeguard = safeguardActive(readOnly, isRequired, selectionWarningEnabled)
    var isChecked by rememberSaveable(value) { mutableStateOf(value != null || isRequired) }

    val clickAction = {
        if (safeguard) showSelectionWarningDialog = true else showDialog = true
    }

    ListItem(
        modifier = Modifier.clickable(onClick = clickAction),
        colors = transparentListItemColors,
        headlineContent = { OptionItemHeadline(option) },
        leadingContent = if (!readOnly) {
            {
                OptionItemCheckbox(
                    checked = isChecked,
                    isRequired = isRequired,
                    safeguardActive = safeguard,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        setValue(if (checked) value ?: emptyList() else null)
                    },
                    onSafeguardClick = { showSelectionWarningDialog = true },
                )
            }
        } else null,
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(option.description)
                if (option.required && value == null) {
                    Text(
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        text = stringResource(R.string.patch_options_value_required),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = { ListOptionItemEditTrailing(readOnly, onClick = clickAction) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListOptionDialog(
    option: Option<List<Serializable>>,
    value: List<Serializable>,
    setValue: (List<Serializable>?) -> Unit,
    onDismiss: () -> Unit,
    readOnly: Boolean,
) {
    val elementType = remember(option.type) { option.type.arguments.first().type!! }

    val items = rememberSaveable(value, saver = snapshotStateListSaver()) {
        value.map(::Item).toMutableStateList()
    }

    val listIsDirty by remember {
        derivedStateOf {
            value.size != items.size || value.zip(items).any { (v, item) -> v != item.value }
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            items.add(to.index, items.removeAt(from.index))
        }

    var deleteMode by rememberSaveable { mutableStateOf(false) }
    val deletionTargets = rememberSaveable(saver = snapshotStateSetSaver()) {
        mutableStateSetOf<Int>()
    }

    val canEditItems = !readOnly && !deleteMode

    var showEmptyListWarning by remember { mutableStateOf(false) }
    var showInvalidListWarning by remember { mutableStateOf(false) }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }

    if (showAddItemDialog) {
        ListItemEditDialog(
            type = elementType,
            title = option.name,
            currentValue = null,
            onDismiss = { showAddItemDialog = false },
            onSubmit = { newValue ->
                items.add(Item(newValue))
                showAddItemDialog = false
            },
        )
    }

    if (showEmptyListWarning) {
        AlertDialog(
            onDismissRequest = { showEmptyListWarning = false },
            title = { Text(stringResource(R.string.patch_options_value_required_list_empty_save_warning_title)) },
            text = { Text(stringResource(R.string.patch_options_value_required_list_empty_save_warning_description)) },
            confirmButton = {
                val currentItems = items.map { it.value }
                TextButton(
                    onClick = {
                        showEmptyListWarning = false
                        if (option.validator(currentItems)) {
                            setValue(currentItems)
                            onDismiss()
                        } else {
                            showInvalidListWarning = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyListWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showInvalidListWarning) {
        AlertDialogExtended(
            onDismissRequest = { showInvalidListWarning = false },
            title = { Text(stringResource(R.string.patch_options_value_list_invalid_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.patch_options_value_list_invalid_dialog_description))
                    ListItem(
                        headlineContent = { OptionItemHeadline(option) },
                        supportingContent = { Text(option.description) },
                        colors = transparentListItemColors,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInvalidListWarning = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.discard_changes))
                }
            }
        )
    }

    val back = remember(listIsDirty, deleteMode) {
        {
            if (deleteMode) {
                deletionTargets.clear()
                deleteMode = false
            } else {
                val currentItems = items.map { it.value }
                if (listIsDirty) {
                    if (option.required && currentItems.isEmpty()) {
                        showEmptyListWarning = true
                    } else if (!option.validator(currentItems)) {
                        showInvalidListWarning = true
                    } else {
                        setValue(currentItems)
                        onDismiss()
                    }
                } else if (option.required && currentItems.isEmpty()) {
                    showEmptyListWarning = true
                } else {
                    onDismiss()
                }
            }
        }
    }

    FullscreenDialog(onDismissRequest = back) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = if (deleteMode) {
                        pluralStringResource(
                            R.plurals.selected_count,
                            deletionTargets.size,
                            deletionTargets.size,
                        )
                    } else {
                        option.name
                    },
                    onBackClick = back,
                    backIcon = {
                        if (deleteMode) {
                            Icon(Icons.Filled.Close, stringResource(R.string.cancel))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        if (!readOnly) ListOptionTopBarActions(
                            deleteMode = deleteMode,
                            onToggleSelectAll = {
                                if (items.size == deletionTargets.size) deletionTargets.clear()
                                else deletionTargets.addAll(items.map { it.key })
                            },
                            onDeleteSelected = {
                                items.removeIf { it.key in deletionTargets }
                                deletionTargets.clear()
                                deleteMode = false
                            },
                            onReset = {
                                items.clear()
                                option.default?.let { items.addAll(it.map(::Item)) }
                            },
                        )
                    },
                )
            },
            floatingActionButton = {
                if (canEditItems) {
                    HapticExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.add)) },
                        icon = { Icon(Icons.Outlined.Add, stringResource(R.string.add)) },
                        expanded = lazyListState.isScrollingUp,
                        onClick = { showAddItemDialog = true },
                    )
                }
            },
        ) { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(paddingValues),
            ) {
                if (readOnly && items.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(R.string.patch_options_no_default_items),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    val interactionSource = remember { MutableInteractionSource() }

                    ReorderableItem(reorderableLazyColumnState, key = item.key) {
                        var showEditDialog by rememberSaveable { mutableStateOf(false) }

                        if (showEditDialog) {
                            ListItemEditDialog(
                                type = elementType,
                                title = option.name,
                                currentValue = item.value,
                                onDismiss = { showEditDialog = false },
                                onSubmit = { newValue ->
                                    items[index] = item.copy(value = newValue)
                                    showEditDialog = false
                                },
                            )
                        }

                        ListItem(
                            modifier = if (readOnly) Modifier
                            else Modifier.combinedClickable(
                                indication = LocalIndication.current,
                                interactionSource = interactionSource,
                                onLongClickLabel = stringResource(R.string.select),
                                onLongClick = {
                                    if (!deleteMode) {
                                        deletionTargets.add(item.key)
                                        deleteMode = true
                                    }
                                },
                                onClick = {
                                    if (deleteMode) {
                                        if (item.key in deletionTargets) {
                                            deletionTargets.remove(item.key)
                                            deleteMode = deletionTargets.isNotEmpty()
                                        } else {
                                            deletionTargets.add(item.key)
                                        }
                                    } else {
                                        showEditDialog = true
                                    }
                                },
                            ),
                            tonalElevation = if (deleteMode && item.key in deletionTargets) {
                                8.dp
                            } else {
                                0.dp
                            },
                            leadingContent = if (!readOnly) {
                                {
                                    TooltipIconButton(
                                        modifier = Modifier.draggableHandle(
                                            interactionSource = interactionSource
                                        ),
                                        onClick = {},
                                        tooltip = stringResource(R.string.drag_handle),
                                    ) {
                                        Icon(
                                            Icons.Filled.DragHandle,
                                            stringResource(R.string.drag_handle),
                                        )
                                    }
                                }
                            } else null,
                            headlineContent = {
                                val isValueEmpty = item.value is String && item.value.isEmpty()
                                if (isValueEmpty) {
                                    Text(
                                        stringResource(R.string.empty),
                                        fontStyle = FontStyle.Italic,
                                    )
                                } else {
                                    Text(item.value.toString())
                                }
                            },
                            trailingContent = if (canEditItems) {
                                {
                                    ListOptionItemEditTrailing(readOnly = false) {
                                        showEditDialog = true
                                    }
                                }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListOptionTopBarActions(
    deleteMode: Boolean,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onReset: () -> Unit,
) {
    if (deleteMode) {
        TooltipIconButton(
            onClick = onToggleSelectAll,
            tooltip = stringResource(R.string.select_deselect_all),
        ) {
            Icon(Icons.Filled.SelectAll, stringResource(R.string.select_deselect_all))
        }
        TooltipIconButton(
            onClick = onDeleteSelected,
            tooltip = stringResource(R.string.delete),
        ) {
            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
        }
    } else {
        TooltipIconButton(
            onClick = onReset,
            tooltip = stringResource(R.string.reset),
        ) {
            Icon(Icons.Filled.Restore, stringResource(R.string.reset))
        }
    }
}

@Composable
private fun ListItemEditDialog(
    type: KType,
    title: String,
    currentValue: Serializable?,
    onDismiss: () -> Unit,
    onSubmit: (Serializable) -> Unit,
) {
    val fs: Filesystem? = if (type == typeOf<String>()) koinInject() else null
    var showFileDialog by rememberSaveable { mutableStateOf(false) }
    var externalValueUpdate: ((String) -> Unit)? by remember { mutableStateOf(null) }

    if (fs != null && showFileDialog) {
        PathSelectorDialog(root = fs.externalFilesDir()) { path ->
            showFileDialog = false
            path?.let {
                externalValueUpdate?.invoke(it.toString())
            }
        }
    }

    val permissionLauncher = if (fs != null) {
        val (contract, permissionName) = fs.permissionContract()
        rememberLauncherForActivityResult(contract) { granted ->
            if (granted) showFileDialog = true
        } to permissionName
    } else null

    when (type) {
        typeOf<Int>() -> IntInputDialog(
            name = title,
            current = currentValue as Int?,
            onSubmit = { if (it == null) onDismiss() else onSubmit(it) },
        )

        typeOf<Long>() -> LongInputDialog(
            name = title,
            current = currentValue as Long?,
            onSubmit = { if (it == null) onDismiss() else onSubmit(it) },
        )

        typeOf<Float>() -> FloatInputDialog(
            name = title,
            current = currentValue as Float?,
            onSubmit = { if (it == null) onDismiss() else onSubmit(it) },
        )

        typeOf<String>() -> TextInputDialog(
            title = title,
            initial = currentValue as String? ?: "",
            placeholder = stringResource(R.string.patch_options_value_list_element),
            validator = { true },
            onConfirm = onSubmit,
            onDismissRequest = onDismiss,
            trailingIcon = { _, onValueChange ->
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TooltipIconButton(
                        onClick = { expanded = true },
                        tooltip = stringResource(R.string.more),
                    ) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Folder, null) },
                            text = { Text(stringResource(R.string.path_selector)) },
                            onClick = {
                                expanded = false
                                externalValueUpdate = onValueChange
                                if (fs!!.hasStoragePermission()) {
                                    showFileDialog = true
                                } else {
                                    permissionLauncher!!.first.launch(permissionLauncher.second)
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }
        )

        else -> onDismiss()
    }
}

@Parcelize
private data class Item(
    val value: Serializable,
    val key: Int = Random.nextInt(),
) : Parcelable