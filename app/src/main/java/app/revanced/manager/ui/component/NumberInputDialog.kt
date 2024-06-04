package app.revanced.manager.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@Composable
private inline fun <T> NumberInputDialog(
    current: T?,
    name: String,
    crossinline onSubmit: (T?) -> Unit,
    crossinline toNumberOrNull: @DisallowComposableCalls String.() -> T?
) {
    var fieldValue by rememberSaveable {
        mutableStateOf(current?.toString().orEmpty())
    }

    val numberFieldValue by remember {
        derivedStateOf { fieldValue.toNumberOrNull() }
    }

    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        title = { Text(name) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                placeholder = {
                    Text(stringResource(R.string.dialog_input_placeholder))
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { numberFieldValue?.let(onSubmit) },
                enabled = numberFieldValue != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun IntInputDialog(current: Int?, name: String, onSubmit: (Int?) -> Unit) =
    NumberInputDialog(current, name, onSubmit, String::toIntOrNull)

@Composable
fun LongInputDialog(current: Long?, name: String, onSubmit: (Long?) -> Unit) =
    NumberInputDialog(current, name, onSubmit, String::toLongOrNull)

@Composable
fun FloatInputDialog(current: Float?, name: String, onSubmit: (Float?) -> Unit) =
    NumberInputDialog(current, name, onSubmit, String::toFloatOrNull)