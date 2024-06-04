package app.revanced.manager.ui.component.patches

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FloatInputDialog
import app.revanced.manager.ui.component.IntInputDialog
import app.revanced.manager.ui.component.LongInputDialog
import app.revanced.manager.util.isScrollingUp
import app.revanced.manager.util.mutableStateSetOf
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.saver.snapshotStateSetSaver
import app.revanced.manager.util.toast
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import kotlin.random.Random

private class OptionEditorScope<T>(
    val option: Option<T>,
    val openDialog: () -> Unit,
    val dismissDialog: () -> Unit,
    val value: T?,
    val setValue: (T?) -> Unit,
) {
    fun submitDialog(value: T?) {
        setValue(value)
        dismissDialog()
    }
}

private interface OptionEditor<T> {
    fun clickAction(scope: OptionEditorScope<T>) = scope.openDialog()

    @Composable
    fun ListItemTrailingContent(scope: OptionEditorScope<T>) {
        IconButton(onClick = { clickAction(scope) }) {
            Icon(Icons.Outlined.Edit, stringResource(R.string.edit))
        }
    }

    @Composable
    fun Dialog(scope: OptionEditorScope<T>)
}

private object StringOptionEditor : OptionEditor<String> {
    @Composable
    override fun Dialog(scope: OptionEditorScope<String>) {
        var showFileDialog by rememberSaveable { mutableStateOf(false) }
        var fieldValue by rememberSaveable(scope.value) {
            mutableStateOf(scope.value.orEmpty())
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

        AlertDialog(
            onDismissRequest = scope.dismissDialog,
            title = { Text(scope.option.title) },
            text = {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    placeholder = {
                        Text(stringResource(R.string.dialog_input_placeholder))
                    },
                    trailingIcon = {
                        var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
                        IconButton(
                            onClick = { showDropdownMenu = true }
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
            confirmButton = {
                TextButton(onClick = { scope.submitDialog(fieldValue) }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = scope.dismissDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private abstract class NumberOptionEditor<T> : OptionEditor<T> {
    @Composable
    protected abstract fun NumberDialog(title: String, current: T?, onSubmit: (T?) -> Unit)

    @Composable
    override fun Dialog(scope: OptionEditorScope<T>) {
        NumberDialog(scope.option.title, scope.value) {
            if (it == null) return@NumberDialog scope.dismissDialog()

            scope.submitDialog(it)
        }
    }
}

private object IntOptionEditor : NumberOptionEditor<Int>() {
    @Composable
    override fun NumberDialog(title: String, current: Int?, onSubmit: (Int?) -> Unit) =
        IntInputDialog(current, title, onSubmit)
}

private object LongOptionEditor : NumberOptionEditor<Long>() {
    @Composable
    override fun NumberDialog(title: String, current: Long?, onSubmit: (Long?) -> Unit) =
        LongInputDialog(current, title, onSubmit)
}

private object FloatOptionEditor : NumberOptionEditor<Float>() {
    @Composable
    override fun NumberDialog(title: String, current: Float?, onSubmit: (Float?) -> Unit) =
        FloatInputDialog(current, title, onSubmit)
}

private object BooleanOptionEditor : OptionEditor<Boolean> {
    override fun clickAction(scope: OptionEditorScope<Boolean>) {
        scope.setValue(!scope.current)
    }

    @Composable
    override fun ListItemTrailingContent(scope: OptionEditorScope<Boolean>) {
        Switch(checked = scope.current, onCheckedChange = scope.setValue)
    }

    @Composable
    override fun Dialog(scope: OptionEditorScope<Boolean>) {
    }

    private val OptionEditorScope<Boolean>.current get() = value ?: false
}

private class PresetOptionEditor<T>(private val innerEditor: OptionEditor<T>) : OptionEditor<T> {
    @Composable
    override fun Dialog(scope: OptionEditorScope<T>) {
        var customDialog by rememberSaveable {
            mutableStateOf(false)
        }

        if (customDialog) {
            val innerScope = OptionEditorScope(
                scope.option,
                openDialog = {},
                dismissDialog = { customDialog = false },
                scope.value,
                scope.setValue
            )

            innerEditor.Dialog(innerScope)
            return
        }

        var selectedPreset by rememberSaveable(scope.value) {
            val presets = scope.option.presets!!

            mutableStateOf(presets.entries.find { it.value == scope.value }?.key)
        }

        AlertDialogExtended(
            onDismissRequest = scope.dismissDialog,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedPreset != null) scope.submitDialog(
                            scope.option.presets?.get(
                                selectedPreset
                            )
                        )
                        else customDialog = true
                    }
                ) {
                    Text(stringResource(if (selectedPreset != null) R.string.save else R.string.continue_))
                }
            },
            dismissButton = {
                TextButton(onClick = scope.dismissDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(scope.option.title) },
            textHorizontalPadding = PaddingValues(horizontal = 0.dp),
            text = {
                val presets = remember(scope.option.presets) {
                    scope.option.presets?.entries?.toList().orEmpty()
                }

                LazyColumn {
                    @Composable
                    fun Item(title: String, value: Any?, presetKey: String?) {
                        ListItem(
                            modifier = Modifier.clickable { selectedPreset = presetKey },
                            headlineContent = { Text(title) },
                            supportingContent = value?.toString()?.let { { Text(it) } },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedPreset == presetKey,
                                    onClick = { selectedPreset = presetKey }
                                )
                            }
                        )
                    }


                    items(presets, key = { it.key }) {
                        Item(it.key, it.value, it.key)
                    }

                    item {
                        Item(stringResource(R.string.option_preset_custom_value), null, null)
                    }
                }
            }
        )
    }
}

private class ListOptionEditor<T>(private val elementEditor: OptionEditor<T>) :
    OptionEditor<List<T>> {
    private fun createElementOption(option: Option<List<T>>) = Option<T>(
        option.title,
        option.key,
        option.description,
        option.required,
        option.type.removeSuffix("Array"),
        null,
        null
    )

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Dialog(scope: OptionEditorScope<List<T>>) {
        val items: SnapshotStateList<Item<T?>> =
            rememberSaveable(scope.value, saver = snapshotStateListSaver()) {
                scope.value?.map { Item<T?>(it) }?.toMutableStateList() ?: mutableStateListOf()
            }

        val listIsDirty by remember {
            derivedStateOf {
                val current = scope.value.orEmpty().filterNotNull()
                if (current.size != items.size) return@derivedStateOf true

                current.forEachIndexed { index, value ->
                    if (value != items[index].value) return@derivedStateOf true
                }

                false
            }
        }

        val lazyListState = rememberLazyListState()
        val reorderableLazyColumnState =
            rememberReorderableLazyColumnState(lazyListState) { from, to ->
                // Update the list
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

        ComposeDialog(
            onDismissRequest = back,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            ),
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = if (deleteMode) pluralStringResource(
                            R.plurals.selected_count,
                            deletionTargets.size,
                            deletionTargets.size
                        ) else scope.option.title,
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
                                IconButton(
                                    onClick = {
                                        if (items.size == deletionTargets.size) deletionTargets.clear()
                                        else deletionTargets.addAll(items.map { it.key })
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.SelectAll,
                                        stringResource(R.string.select_deselect_all)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        items.removeIf { it.key in deletionTargets }
                                        deletionTargets.clear()
                                        deleteMode = false
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        stringResource(R.string.delete)
                                    )
                                }
                            } else {
                                IconButton(onClick = items::clear) {
                                    Icon(Icons.Outlined.Restore, stringResource(R.string.reset))
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (deleteMode) return@Scaffold

                    ExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.add)) },
                        icon = { Icon(Icons.Outlined.Add, null) },
                        expanded = lazyListState.isScrollingUp,
                        onClick = { items.add(Item(null)) }
                    )
                }
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
                            var showDialog by rememberSaveable { mutableStateOf(false) }

                            val elementScope = OptionEditorScope(
                                createElementOption(scope.option),
                                openDialog = { showDialog = true },
                                dismissDialog = { showDialog = false },
                                item.value,
                                setValue = { items[index] = item.copy(value = it) },
                            )

                            if (showDialog)
                                elementEditor.Dialog(elementScope)

                            ListItem(
                                modifier = Modifier.combinedClickable(
                                    indication = LocalIndication.current,
                                    interactionSource = interactionSource,
                                    onLongClickLabel = stringResource(R.string.select),
                                    onLongClick = {
                                        deletionTargets.add(item.key)
                                        deleteMode = true
                                    },
                                    onClick = {
                                        if (!deleteMode) {
                                            elementEditor.clickAction(elementScope)
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
                                    IconButton(
                                        modifier = Modifier.draggableHandle(interactionSource = interactionSource),
                                        onClick = {},
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
                                    elementEditor.ListItemTrailingContent(
                                        elementScope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private data class Item<T>(val value: T, val key: Int = Random.nextInt())
}

private object UnknownTypeEditor : OptionEditor<Any>, KoinComponent {
    override fun clickAction(scope: OptionEditorScope<Any>) =
        get<Application>().toast("Unknown type: ${scope.option.type}")

    @Composable
    override fun Dialog(scope: OptionEditorScope<Any>) {
    }
}

private val optionEditors = mapOf(
    "Boolean" to BooleanOptionEditor,
    "String" to StringOptionEditor,
    "Int" to IntOptionEditor,
    "Long" to LongOptionEditor,
    "Float" to FloatOptionEditor,
    "BooleanArray" to ListOptionEditor(BooleanOptionEditor),
    "StringArray" to ListOptionEditor(StringOptionEditor),
    "IntArray" to ListOptionEditor(IntOptionEditor),
    "LongArray" to ListOptionEditor(LongOptionEditor),
    "FloatArray" to ListOptionEditor(FloatOptionEditor),
)

@Composable
fun <T> OptionItem(option: Option<T>, value: T?, setValue: (T?) -> Unit) {
    val editor = remember(option.type) {
        @Suppress("UNCHECKED_CAST")
        val baseOptionEditor =
            optionEditors.getOrDefault(option.type, UnknownTypeEditor) as OptionEditor<T>

        if (option.type != "Boolean" && option.presets != null) PresetOptionEditor(baseOptionEditor)
        else baseOptionEditor
    }

    var showDialog by rememberSaveable { mutableStateOf(false) }

    val scope = OptionEditorScope(
        option,
        openDialog = { showDialog = true },
        dismissDialog = { showDialog = false },
        value,
        setValue
    )

    if (showDialog)
        editor.Dialog(scope)

    ListItem(
        modifier = Modifier.clickable { editor.clickAction(scope) },
        headlineContent = { Text(option.title) },
        supportingContent = { Text(option.description) },
        trailingContent = { editor.ListItemTrailingContent(scope) }
    )
}