package app.revanced.manager.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@Composable
fun TextInputDialog(
    initial: String,
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    validator: (String) -> Boolean = String::isNotEmpty,
) {
    val (value, setValue) = rememberSaveable(initial) {
        mutableStateOf(initial)
    }
    val valid = remember(value, validator) {
        validator(value)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = valid
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(title)
        },
        text = {
            TextField(value = value, onValueChange = setValue)
        }
    )
}