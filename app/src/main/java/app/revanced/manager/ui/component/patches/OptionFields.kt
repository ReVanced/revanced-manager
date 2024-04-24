package app.revanced.manager.ui.component.patches

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.toast
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

// Composable functions do not support function references, so we have to use composable lambdas instead.
private typealias OptionImpl = @Composable (Option, Any?, (Any?) -> Unit) -> Unit

private class OptionEditorScope<T>(
    val option: Option,
    val openDialog: () -> Unit,
    val dismissDialog: () -> Unit,
    val value: T?,
    val setValue: (T?) -> Unit,
) {
    fun dialogSubmit(value: T) {
        setValue(value)
        dismissDialog()
    }
}

private interface OptionEditor<T> {
    fun clickAction(scope: OptionEditorScope<T>) = scope.openDialog()

    @Composable
    fun ListItemTrailingContent(scope: OptionEditorScope<T>) {
        IconButton(onClick = scope.openDialog) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.edit)
            )
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
                TextButton(onClick = { scope.dialogSubmit(fieldValue) }) {
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

private class ArrayOptionEditor<T>(private val elementEditor: OptionEditor<T>) :
    OptionEditor<Array<T>> {
    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Dialog(scope: OptionEditorScope<Array<T>>) {
        val items: SnapshotStateList<T?> =
            rememberSaveable(scope.value, saver = snapshotStateListSaver()) {
                scope.value?.let { mutableStateListOf(*it) } ?: mutableStateListOf()
            }

        val lazyListState = rememberLazyListState()
        val reorderableLazyColumnState =
            rememberReorderableLazyColumnState(lazyListState) { from, to ->
                // Update the list
                items.add(to.index, items.removeAt(from.index))
            }

        ComposeDialog(
            onDismissRequest = scope.dismissDialog,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            ),
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = scope.option.title,
                        onBackClick = scope.dismissDialog,
                        backIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    items.add(null)
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    stringResource(R.string.add)
                                )
                            }
                            IconButton(
                                onClick = {
                                    TODO("implement deletion")
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    stringResource(R.string.delete)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            scope.dialogSubmit(items.toTypedArray<Any?>() as Array<T>)
                        }
                    ) {
                        Icon(Icons.Default.Save, stringResource(R.string.save))
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(paddingValues),
                ) {
                    itemsIndexed(items) { index, item ->
                        ReorderableItem(reorderableLazyColumnState, key = index) {
                            OptionListItem(
                                scope.option,
                                elementEditor,
                                value = item,
                                setValue = { items[index] = it as T },
                                headlineContent = { Text(item.toString()) }, // TODO: improve this.
                                supportingContent = null,
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.DragHandle,
                                        null,
                                        modifier = Modifier.draggableHandle()
                                    ) // TODO: accessibility description
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private object UnknownTypeEditor : OptionEditor<Any>, KoinComponent {
    override fun clickAction(scope: OptionEditorScope<Any>) =
        get<Application>().toast("Unknown type: ${scope.option.type}")

    @Composable
    override fun Dialog(scope: OptionEditorScope<Any>) {
    }
}

@Composable
private fun <T> OptionListItem(
    option: Option,
    editor: OptionEditor<T>,
    value: Any?,
    setValue: (Any?) -> Unit,
    headlineContent: @Composable () -> Unit = { Text(option.title) },
    supportingContent: (@Composable () -> Unit)? = { Text(option.description) },
    leadingContent: (@Composable () -> Unit)? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val scope = OptionEditorScope(
        option,
        openDialog = { showDialog = true },
        dismissDialog = { showDialog = false },
        value as T?,
        setValue = setValue,
    )

    if (showDialog)
        editor.Dialog(scope)

    ListItem(
        modifier = Modifier.clickable(onClick = { editor.clickAction(scope) }),
        leadingContent = leadingContent,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = { editor.ListItemTrailingContent(scope) }
    )
}

private val optionEditors = mapOf<String, OptionEditor<*>>(
    "Boolean" to BooleanOptionEditor,
    "String" to StringOptionEditor,
    "StringArray" to ArrayOptionEditor(StringOptionEditor),
)

@Composable
fun OptionItem(option: Option, value: Any?, setValue: (Any?) -> Unit) {
    val editor = remember(option.type) {
        optionEditors.getOrDefault(option.type, UnknownTypeEditor)
    }

    OptionListItem(option, editor, value, setValue)
}