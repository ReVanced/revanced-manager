package app.revanced.manager.ui.component.patches

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import app.revanced.manager.data.platform.FileSystem
import app.revanced.manager.patcher.patch.Option
import app.revanced.patcher.patch.PatchOption
import org.koin.compose.rememberKoinInject

/**
 * [Composable] functions do not support function references, so we have to use composable lambdas instead.
 */
private typealias OptionField = @Composable (Any?, (Any?) -> Unit) -> Unit

private val StringField: OptionField = { value, setValue ->
    val fs: FileSystem = rememberKoinInject()
    var showFileDialog by rememberSaveable { mutableStateOf(false) }
    val (contract, permissionName) = fs.permissionContract()
    val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
        showFileDialog = it
    }
    val current = value as? String

    if (showFileDialog) {
        PathSelectorDialog(
            root = fs.externalFilesDir()
        ) {
            showFileDialog = false
            it?.let { path ->
                setValue(path.toString())
            }
        }
    }

    Column {
        TextField(value = current ?: "", onValueChange = setValue)
        Button(onClick = {
            if (fs.hasStoragePermission()) {
                showFileDialog = true
            } else {
                permissionLauncher.launch(permissionName)
            }
        }) {
            Icon(Icons.Filled.FileOpen, null)
            Text("Select file or folder")
        }
    }
}

private val BooleanField: OptionField = { value, setValue ->
    val current = value as? Boolean
    Switch(checked = current ?: false, onCheckedChange = setValue)
}

private val UnknownField: OptionField = { _, _ ->
    Text("This type has not been implemented")
}

@Composable
fun OptionField(option: Option, value: Any?, setValue: (Any?) -> Unit) {
    val implementation = remember(option.type) {
        when (option.type) {
            // These are the only two types that are currently used by the official patches.
            PatchOption.StringOption::class.java -> StringField
            PatchOption.BooleanOption::class.java -> BooleanField
            else -> UnknownField
        }
    }

    implementation(value, setValue)
}