@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.revanced.manager.ui.component.patches

import android.app.Application
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
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
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.ui.component.haptics.HapticSwitch
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.saver.snapshotStateSetSaver
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import kotlinx.parcelize.Parcelize
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.Serializable
import kotlin.random.Random
import kotlin.reflect.typeOf

private class OptionEditorScope<T : Any>(
    private val editor: OptionEditor<T>,
    val option: Option<T>,
    val openDialog: () -> Unit,
    val dismissDialog: () -> Unit,
    val selectionWarningEnabled: Boolean,
    val showSelectionWarning: () -> Unit,
    val value: T?,
    val setValue: (T?) -> Unit,
    val readOnly: Boolean
) {
    enum class Mode {
        NULL,
        DEFAULT,
        PRESET,
        CUSTOM
    }

    val mode: Mode = when {
        !option.required && value == null -> Mode.NULL
        value == option.default -> Mode.DEFAULT
        option.presets?.values?.contains(value) == true -> Mode.PRESET
        else -> Mode.CUSTOM
    }

    fun submitDialog(value: T?) {
        setValue(value)
        dismissDialog()
    }

    fun checkSafeguard(block: () -> Unit) {
        if (readOnly)
            block()
        else if (!option.required && selectionWarningEnabled)
            showSelectionWarning()
        else
            block()
    }

    fun clickAction() {
        if (!readOnly) checkSafeguard {
            editor.clickAction(this)
        }
    }

    @Composable
    fun ListItemTrailingContent() = editor.ListItemTrailingContent(this)

    @Composable
    fun Dialog() = editor.Dialog(this)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun SelectionDialog(
        customContent: @Composable () -> Unit,
        onCustomSave: () -> Unit,
        customSaveEnabled: Boolean = true
    ) {
        var selectedMode by rememberSaveable { mutableStateOf(mode) }
        var selectedPresetKey by rememberSaveable {
            mutableStateOf(option.presets?.entries?.find { it.value == value }?.key)
        }
        var showCustomPage by rememberSaveable { mutableStateOf(mode == Mode.CUSTOM) }

        AlertDialogExtended(
            onDismissRequest = dismissDialog,
            title = { Text(option.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showCustomPage) onCustomSave()
                        else {
                            when (selectedMode) {
                                Mode.NULL -> submitDialog(null)
                                Mode.DEFAULT -> submitDialog(option.default)
                                Mode.PRESET -> submitDialog(option.presets?.get(selectedPresetKey))
                                Mode.CUSTOM -> onCustomSave()
                            }
                        }
                    },
                    enabled = !showCustomPage || customSaveEnabled,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissDialog, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            },
            tertiaryButton = {
                TextButton(
                    onClick = { showCustomPage = !showCustomPage },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(if (showCustomPage) R.string.patch_options_presets else R.string.patch_options_custom))
                }
            },
            textHorizontalPadding = PaddingValues(horizontal = 0.dp),
            text = {
                if (showCustomPage) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        customContent()
                    }
                } else {
                    LazyColumn {
                        if (!option.required) {
                            item {
                                ListItem(
                                    onClick = { selectedMode = Mode.NULL },
                                    content = { Text(stringResource(R.string.patch_options_set_null)) },
                                    supportingContent = { Text(stringResource(R.string.patch_options_set_null_description)) },
                                    leadingContent = {
                                        HapticRadioButton(
                                            selected = selectedMode == Mode.NULL,
                                            onClick = { selectedMode = Mode.NULL }
                                        )
                                    },
                                    colors = transparentListItemColors
                                )
                            }
                        }

                        option.default?.let { default ->
                            item {
                                ListItem(
                                    onClick = { selectedMode = Mode.DEFAULT },
                                    content = { Text(stringResource(R.string.patch_options_set_default)) },
                                    supportingContent = { Text(default.toString()) },
                                    leadingContent = {
                                        HapticRadioButton(
                                            selected = selectedMode == Mode.DEFAULT,
                                            onClick = { selectedMode = Mode.DEFAULT }
                                        )
                                    },
                                    colors = transparentListItemColors
                                )
                            }
                        }

                        option.presets?.forEach { (key, presetValue) ->
                            item {
                                ListItem(
                                    onClick = {
                                        selectedMode = Mode.PRESET
                                        selectedPresetKey = key
                                    },
                                    content = { Text(key) },
                                    supportingContent = presetValue?.let { { Text(it.toString()) } },
                                    leadingContent = {
                                        HapticRadioButton(
                                            selected = selectedMode == Mode.PRESET && selectedPresetKey == key,
                                            onClick = {
                                                selectedMode = Mode.PRESET
                                                selectedPresetKey = key
                                            }
                                        )
                                    },
                                    colors = transparentListItemColors
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

private interface OptionEditor<T : Any> {
    fun clickAction(scope: OptionEditorScope<T>) = scope.openDialog()

    fun shouldHintSetValue() = true

    @Composable
    fun ListItemTrailingContent(scope: OptionEditorScope<T>) {
    }

    @Composable
    fun Dialog(scope: OptionEditorScope<T>)
}

private inline fun <reified T : Serializable> OptionEditor<T>.toMapEditorElements() = arrayOf(
    typeOf<T>() to this,
    typeOf<List<T>>() to ListOptionEditor(this)
)

private val optionEditors = mapOf(
    *BooleanOptionEditor.toMapEditorElements(),
    *StringOptionEditor.toMapEditorElements(),
    *IntOptionEditor.toMapEditorElements(),
    *LongOptionEditor.toMapEditorElements(),
    *FloatOptionEditor.toMapEditorElements()
)

@Composable
private inline fun <T : Any> WithOptionEditor(
    editor: OptionEditor<T>,
    option: Option<T>,
    value: T?,
    noinline setValue: (T?) -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean,
    crossinline onDismissDialog: @DisallowComposableCalls () -> Unit = {},
    block: OptionEditorScope<T>.() -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectionWarningDialog by rememberSaveable { mutableStateOf(false) }

    val scope = remember(editor, option, value, setValue, selectionWarningEnabled, readOnly) {
        OptionEditorScope(
            editor,
            option,
            openDialog = { showDialog = true },
            dismissDialog = {
                showDialog = false
                onDismissDialog()
            },
            selectionWarningEnabled,
            showSelectionWarning = { showSelectionWarningDialog = true },
            value,
            setValue,
            readOnly
        )
    }

    if (showSelectionWarningDialog)
        SelectionWarningDialog(
            onDismiss = { showSelectionWarningDialog = false }
        )

    if (showDialog) scope.Dialog()

    scope.block()
}

@Composable
fun <T : Any> OptionItem(
    option: Option<T>,
    value: T?,
    setValue: (T?) -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean = false
) {
    val editor = remember(option.type, option.presets) {
        @Suppress("UNCHECKED_CAST")
        optionEditors.getOrDefault(option.type, UnknownTypeEditor) as OptionEditor<T>
    }

    WithOptionEditor(editor, option, value, setValue, selectionWarningEnabled, readOnly) {
        ListItem(
            onClick = ::clickAction,
            content = { Text(option.name) },
            supportingContent = {
                Column {
                    Text(option.description)
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (!readOnly && editor.shouldHintSetValue() && value != null) {
                            Text(
                                stringResource(R.string.patch_options_user_set_value, value.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        option.default?.let {
                            Text(
                                stringResource(R.string.patch_options_default_value, it.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    if (option.required && value == null) Text(
                        stringResource(R.string.option_required),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingContent = if (!readOnly) {
                { ListItemTrailingContent() }
            } else null,
        )
    }
}

private fun <T> optionValueLabelPlain(
    option: Option<T>,
    value: T?,
    unsetLabel: String
): String {
    val presetLabel = option.presets?.entries?.firstOrNull { it.value == value }?.key

    return when {
        presetLabel != null && value != null -> "$presetLabel ($value)"
        presetLabel != null -> presetLabel
        value == null -> unsetLabel
        else -> value.toString()
    }
}

@Composable
private fun <T> optionValueLabel(
    option: Option<T>,
    value: T?,
) = optionValueLabelPlain(
    option = option,
    value = value,
    unsetLabel = stringResource(if (value == null && !option.required) R.string.patch_options_set_null else R.string.field_not_set)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReadonlyOptionDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(title) },
    text = content,
    confirmButton = {
        TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.ok))
        }
    }
)

private object StringOptionEditor : OptionEditor<String> {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun Dialog(scope: OptionEditorScope<String>) {
        if (scope.readOnly) {
            ReadonlyOptionDialog(
                title = scope.option.name,
                onDismissRequest = scope.dismissDialog,
            ) {
                OutlinedTextField(
                    value = optionValueLabel(scope.option, scope.value),
                    onValueChange = {},
                    enabled = false,
                )
            }
            return
        }

        var showFileDialog by rememberSaveable { mutableStateOf(false) }
        var fieldValue by rememberSaveable(scope.value) {
            mutableStateOf(scope.value.orEmpty())
        }
        val validatorFailed by remember {
            derivedStateOf { !scope.option.validator(fieldValue) }
        }

        val fs: Filesystem = koinInject()
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

        scope.SelectionDialog(
            customContent = {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    placeholder = {
                        Text(stringResource(R.string.dialog_input_placeholder))
                    },
                    isError = validatorFailed,
                    supportingText = {
                        if (validatorFailed) {
                            Text(
                                stringResource(R.string.input_dialog_value_invalid),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
                        TooltipIconButton(
                            onClick = { showDropdownMenu = true },
                            tooltip = stringResource(R.string.string_option_menu_description)
                        ) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                stringResource(R.string.string_option_menu_description)
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
            onCustomSave = { scope.submitDialog(fieldValue) },
            customSaveEnabled = !validatorFailed
        )
    }
}

private abstract class NumberOptionEditor<T : Number> : OptionEditor<T> {
    @Composable
    abstract fun NumberDialog(
        title: String,
        current: T?,
        validator: (T?) -> Boolean,
        onSubmit: (T?) -> Unit,
        onCancel: () -> Unit
    )

    @Composable
    override fun Dialog(scope: OptionEditorScope<T>) {
        if (scope.readOnly) {
            ReadonlyOptionDialog(
                title = scope.option.name,
                onDismissRequest = scope.dismissDialog,
            ) {
                OutlinedTextField(
                    value = optionValueLabel(scope.option, scope.value),
                    onValueChange = {},
                    enabled = false,
                )
            }
            return
        }

        var fieldValue by rememberSaveable(scope.value) {
            mutableStateOf(scope.value)
        }
        var isValid by remember { mutableStateOf(true) }

        scope.SelectionDialog(
            customContent = {
                NumberDialog(
                    title = scope.option.name,
                    current = fieldValue,
                    validator = scope.option.validator,
                    onSubmit = {
                        fieldValue = it
                        isValid = it != null && scope.option.validator(it)
                    },
                    onCancel = { scope.dismissDialog() }
                )
            },
            onCustomSave = { scope.submitDialog(fieldValue) },
            customSaveEnabled = isValid && fieldValue != null
        )
    }
}

private object IntOptionEditor : NumberOptionEditor<Int>() {
    @Composable
    override fun NumberDialog(
        title: String,
        current: Int?,
        validator: (Int?) -> Boolean,
        onSubmit: (Int?) -> Unit,
        onCancel: () -> Unit
    ) = IntInputDialog(current, title, unit = null, validator, onSubmit)
}

private object LongOptionEditor : NumberOptionEditor<Long>() {
    @Composable
    override fun NumberDialog(
        title: String,
        current: Long?,
        validator: (Long?) -> Boolean,
        onSubmit: (Long?) -> Unit,
        onCancel: () -> Unit
    ) = LongInputDialog(current, title, unit = null, validator, onSubmit)
}

private object FloatOptionEditor : NumberOptionEditor<Float>() {
    @Composable
    override fun NumberDialog(
        title: String,
        current: Float?,
        validator: (Float?) -> Boolean,
        onSubmit: (Float?) -> Unit,
        onCancel: () -> Unit
    ) = FloatInputDialog(current, title, unit = null, validator, onSubmit)
}

private object BooleanOptionEditor : OptionEditor<Boolean> {
    override fun shouldHintSetValue() = false

    override fun clickAction(scope: OptionEditorScope<Boolean>) {
        if (scope.readOnly) return
        scope.setValue(!scope.current)
    }

    @Composable
    override fun ListItemTrailingContent(scope: OptionEditorScope<Boolean>) {
        HapticSwitch(
            checked = scope.current,
            onCheckedChange = { value ->
                scope.checkSafeguard {
                    scope.setValue(value)
                }
            },
            enabled = !scope.readOnly,
            thumbContent = if (scope.current) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        )
    }

    @Composable
    override fun Dialog(scope: OptionEditorScope<Boolean>) {
        if (scope.readOnly) return
        scope.SelectionDialog(
            customContent = {
                ListItem(
                    onClick = { scope.setValue(!scope.current) },
                    content = { Text(stringResource(if (scope.current) R.string.onboarding_skip else R.string.add)) },
                    trailingContent = { ListItemTrailingContent(scope) },
                    colors = transparentListItemColors
                )
            },
            onCustomSave = { scope.dismissDialog() }
        )
    }

    private val OptionEditorScope<Boolean>.current get() = value ?: false
}

private object UnknownTypeEditor : OptionEditor<Any>, KoinComponent {
    override fun clickAction(scope: OptionEditorScope<Any>) =
        get<Application>().toast("Unknown type: ${scope.option.type}")

    @Composable
    override fun Dialog(scope: OptionEditorScope<Any>) {
    }
}

private class ListOptionEditor<T : Serializable>(private val elementEditor: OptionEditor<T>) :
    OptionEditor<List<T>> {
    private fun createElementOption(option: Option<List<T>>) = Option<T>(
        option.name,
        option.description,
        option.required,
        option.type.arguments.first().type!!,
        null,
        null
    ) { true }

    @OptIn(
        ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalMaterial3ExpressiveApi::class
    )
    @Composable
    override fun Dialog(scope: OptionEditorScope<List<T>>) {
        if (scope.readOnly) {
            FullscreenDialog(
                onDismissRequest = scope.dismissDialog,
            ) {
                Scaffold(
                    topBar = {
                        AppTopBar(
                            title = scope.option.name,
                            onBackClick = scope.dismissDialog,
                        )
                    }
                ) { paddingValues ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(paddingValues),
                    ) {
                        val items = scope.value.orEmpty()
                        if (items.isEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            stringResource(R.string.empty),
                                            fontStyle = FontStyle.Italic
                                        )
                                    },
                                    colors = transparentListItemColors
                                )
                            }
                        } else {
                            items(items) { item ->
                                ListItem(
                                    headlineContent = { Text(item.toString()) },
                                    colors = transparentListItemColors
                                )
                            }
                        }
                    }
                }
            }
            return
        }

        val items =
            rememberSaveable(scope.value, saver = snapshotStateListSaver()) {
                // We need a key for each element in order to support dragging.
                scope.value?.map(::Item)?.toMutableStateList() ?: mutableStateListOf()
            }
        val listIsDirty by remember {
            derivedStateOf {
                val current = scope.value.orEmpty()
                if (current.size != items.size) return@derivedStateOf true

                current.forEachIndexed { index, value ->
                    if (value != items[index].value) return@derivedStateOf true
                }

                false
            }
        }

        val lazyListState = rememberLazyListState()
        val reorderableLazyColumnState =
            rememberReorderableLazyListState(lazyListState) { from, to ->
                items.add(to.index, items.removeAt(from.index))
            }

        var deleteMode by rememberSaveable {
            mutableStateOf(false)
        }
        val deletionTargets = rememberSaveable(saver = snapshotStateSetSaver()) {
            mutableStateSetOf<Int>()
        }

        val back = back@{
            if (deleteMode) {
                deletionTargets.clear()
                deleteMode = false
                return@back
            }

            if (!listIsDirty) {
                scope.dismissDialog()
                return@back
            }

            scope.submitDialog(items.mapNotNull { it.value })
        }

        FullscreenDialog(
            onDismissRequest = back,
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = if (deleteMode) pluralStringResource(
                            R.plurals.selected_count,
                            deletionTargets.size,
                            deletionTargets.size
                        ) else scope.option.name,
                        onBackClick = back,
                        backIcon = {
                            if (deleteMode) {
                                return@AppTopBar Icon(
                                    Icons.Filled.Close,
                                    stringResource(R.string.cancel)
                                )
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        },
                        actions = {
                            if (deleteMode) {
                                TooltipIconButton(
                                    onClick = {
                                        if (items.size == deletionTargets.size) deletionTargets.clear()
                                        else deletionTargets.addAll(items.map { it.key })
                                    },
                                    tooltip = stringResource(R.string.select_deselect_all)
                                ) {
                                    Icon(
                                        Icons.Filled.SelectAll,
                                        stringResource(R.string.select_deselect_all)
                                    )
                                }
                                TooltipIconButton(
                                    onClick = {
                                        items.removeIf { it.key in deletionTargets }
                                        deletionTargets.clear()
                                        deleteMode = false
                                    },
                                    tooltip = stringResource(R.string.delete)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        stringResource(R.string.delete)
                                    )
                                }
                            } else {
                                TooltipIconButton(
                                    onClick = items::clear,
                                    tooltip = stringResource(R.string.reset)
                                ) {
                                    Icon(
                                        Icons.Filled.Restore,
                                        stringResource(R.string.reset)
                                    )
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (deleteMode) return@Scaffold

                    HapticExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.add)) },
                        icon = {
                            Icon(
                                Icons.Outlined.Add,
                                stringResource(R.string.add)
                            )
                        },
                        expanded = lazyListState.isScrollingUp,
                        onClick = { items.add(Item(null)) }
                    )
                }
            ) { paddingValues ->
                val elementOption = remember(scope.option) { createElementOption(scope.option) }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(paddingValues),
                ) {
                    itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                        val interactionSource = remember { MutableInteractionSource() }

                        ReorderableItem(reorderableLazyColumnState, key = item.key) {
                            WithOptionEditor(
                                elementEditor,
                                elementOption,
                                value = item.value,
                                setValue = { items[index] = item.copy(value = it) },
                                selectionWarningEnabled = scope.selectionWarningEnabled,
                                readOnly = false
                            ) {
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
                                            if (!deleteMode) {
                                                clickAction()
                                                return@combinedClickable
                                            }

                                            if (item.key in deletionTargets) {
                                                deletionTargets.remove(
                                                    item.key
                                                )
                                                deleteMode = deletionTargets.isNotEmpty()
                                            } else deletionTargets.add(item.key)
                                        },
                                    ),
                                    tonalElevation = if (deleteMode && item.key in deletionTargets) 8.dp else 0.dp,
                                    leadingContent = {
                                        TooltipIconButton(
                                            modifier = Modifier.draggableHandle(interactionSource = interactionSource),
                                            onClick = {},
                                            tooltip = stringResource(R.string.drag_handle),
                                        ) {
                                            Icon(
                                                Icons.Filled.DragHandle,
                                                stringResource(R.string.drag_handle)
                                            )
                                        }
                                    },
                                    headlineContent = {
                                        if (item.value == null) return@ListItem Text(
                                            stringResource(R.string.empty),
                                            fontStyle = FontStyle.Italic
                                        )

                                        Text(item.value.toString())
                                    },
                                    trailingContent = {
                                        ListItemTrailingContent()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Parcelize
    private data class Item<T : Serializable>(val value: T?, val key: Int = Random.nextInt()) :
        Parcelable
}
