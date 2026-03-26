@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package app.revanced.manager.ui.component.patches

import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
): String = when {
    v == null -> ""
    v == true -> boolTrueStr
    v == false -> boolFalseStr
    else -> v.toString()
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> parseValue(str: String, type: KType): T? {
    if (str.isEmpty() && type != typeOf<String>()) return null
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
    localChecked: Boolean,
    isRequired: Boolean,
    safeguardActive: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSafeguardClick: () -> Unit,
) {
    Box {
        Checkbox(
            checked = localChecked,
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
private fun OptionItemSupporting(option: Option<*>, value: Any?) {
    Column {
        Text(option.description)
        if (option.required && value == null) {
            Text(
                style = MaterialTheme.typography.labelLargeEmphasized,
                text = stringResource(R.string.patch_options_value_required),
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OptionItemEditTrailing(readOnly: Boolean, onClick: () -> Unit) {
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
fun <T : Any> OptionItem(
    option: Option<T>,
    value: T?,
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
                    localChecked = isChecked,
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
                            } else if (isValid) {
                                localValue = parsedValue
                                setValue(parsedValue)
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
    readOnly: Boolean,
    safeguard: Boolean,
    canShowError: Boolean,
    isBoolean: Boolean,
    isString: Boolean,
    keyboardType: KeyboardType,
    fs: Filesystem?,
    permissionLauncher: Pair<androidx.activity.result.ActivityResultLauncher<String>, String>?,
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
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                maxLines = 5,
                value = displayText,
                onValueChange = { if (!safeguard) onValueChange(it) },
                enabled = !readOnly && isChecked,
                isError = canShowError,
                readOnly = isBoolean,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                supportingText = if (canShowError) {
                    { Text(stringResource(R.string.patch_options_value_invalid)) }
                } else null,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
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
            value = value ?: emptyList(),
            setValue = setValue,
            selectionWarningEnabled = selectionWarningEnabled,
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
                    localChecked = isChecked,
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
        supportingContent = { OptionItemSupporting(option, value) },
        trailingContent = { OptionItemEditTrailing(readOnly, onClick = clickAction) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListOptionDialog(
    option: Option<List<Serializable>>,
    value: List<Serializable>,
    setValue: (List<Serializable>?) -> Unit,
    selectionWarningEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val elementType = remember(option.type) { option.type.arguments.first().type!! }

    val items = rememberSaveable(value, saver = snapshotStateListSaver()) {
        value.map(::Item).toMutableStateList()
    }

    val listIsDirty by remember {
        derivedStateOf {
            value.size != items.size ||
                    value.zip(items).any { (v, item) -> v != item.value }
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

    val back = remember(listIsDirty) {
        {
            if (deleteMode) {
                deletionTargets.clear()
                deleteMode = false
            } else {
                if (listIsDirty) setValue(items.mapNotNull { it.value })
                onDismiss()
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
                        ListOptionTopBarActions(
                            deleteMode = deleteMode,
                            allSelected = items.size == deletionTargets.size,
                            onToggleSelectAll = {
                                if (items.size == deletionTargets.size) deletionTargets.clear()
                                else deletionTargets.addAll(items.map { it.key })
                            },
                            onDeleteSelected = {
                                items.removeIf { it.key in deletionTargets }
                                deletionTargets.clear()
                                deleteMode = false
                            },
                            onClearAll = items::clear,
                        )
                    },
                )
            },
            floatingActionButton = {
                if (!deleteMode) {
                    HapticExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.add)) },
                        icon = { Icon(Icons.Outlined.Add, stringResource(R.string.add)) },
                        expanded = lazyListState.isScrollingUp,
                        onClick = { items.add(Item(null)) },
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
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    val interactionSource = remember { MutableInteractionSource() }

                    ReorderableItem(reorderableLazyColumnState, key = item.key) {
                        var showEditDialog by rememberSaveable { mutableStateOf(false) }

                        if (showEditDialog) {
                            ListItemEditDialog(
                                elementType = elementType,
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
                            modifier = Modifier.combinedClickable(
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
                            leadingContent = {
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
                            },
                            headlineContent = {
                                if (item.value == null) {
                                    Text(
                                        stringResource(R.string.empty),
                                        fontStyle = FontStyle.Italic,
                                    )
                                } else {
                                    Text(item.value.toString())
                                }
                            },
                            trailingContent = if (!deleteMode) {
                                {
                                    OptionItemEditTrailing(readOnly = false) {
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
    allSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit,
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
            onClick = onClearAll,
            tooltip = stringResource(R.string.reset),
        ) {
            Icon(Icons.Filled.Restore, stringResource(R.string.reset))
        }
    }
}

@Composable
private fun ListItemEditDialog(
    elementType: KType,
    title: String,
    currentValue: Serializable?,
    onDismiss: () -> Unit,
    onSubmit: (Serializable?) -> Unit,
) {
    when (elementType) {
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
            onConfirm = onSubmit,
            onDismissRequest = onDismiss,
        )

        else -> onDismiss()
    }
}

@Parcelize
private data class Item(
    val value: Serializable?,
    val key: Int = Random.nextInt(),
) : Parcelable