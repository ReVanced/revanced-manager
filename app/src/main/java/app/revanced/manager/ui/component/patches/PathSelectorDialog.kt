package app.revanced.manager.ui.component.patches

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.util.saver.PathSaver
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathSelectorDialog(root: Path, onSelect: (Path?) -> Unit) {
    var currentDirectory by rememberSaveable(root, stateSaver = PathSaver) { mutableStateOf(root) }
    val notAtRootDir = remember(currentDirectory) {
        currentDirectory != root
    }
    val everything = remember(currentDirectory) {
        currentDirectory.listDirectoryEntries()
    }
    val directories = remember(everything) {
        everything.filter { it.isDirectory() }
    }
    val files = remember(everything) {
        everything.filter { it.isRegularFile() }
    }

    Dialog(
        onDismissRequest = { onSelect(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.select_file),
                    onBackClick = { onSelect(null) }
                )
            }
        ) { paddingValues ->
            BackHandler(enabled = notAtRootDir) {
                currentDirectory = currentDirectory.parent
            }

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = currentDirectory.toString())
                Row(
                    modifier = Modifier.clickable { onSelect(currentDirectory) }
                ) {
                    Text("(Use this directory)")
                }
                if (notAtRootDir) {
                    Row(
                        modifier = Modifier.clickable { currentDirectory = currentDirectory.parent }
                    ) {
                        Text("Previous directory")
                    }
                }

                directories.forEach {
                    Row(
                        modifier = Modifier.clickable { currentDirectory = it }
                    ) {
                        Icon(Icons.Filled.Folder, null)
                        Text(text = it.name)
                    }
                }
                files.forEach {
                    Row(
                        modifier = Modifier.clickable { onSelect(it) }
                    ) {
                        Icon(Icons.Filled.FileOpen, null)
                        Text(text = it.name)
                    }
                }
            }
        }
    }
}