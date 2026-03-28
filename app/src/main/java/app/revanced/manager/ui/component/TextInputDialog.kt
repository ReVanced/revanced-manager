package app.revanced.manager.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextInputDialog(
    initial: String,
    title: String,
    placeholder: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    validator: (String) -> Boolean = String::isNotEmpty,
    trailingIcon: @Composable ((value: String, onValueChange: (String) -> Unit) -> Unit)? = null,
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
                enabled = valid,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(title)
        },
        text = {
            TextField(
                value = value,
                onValueChange = setValue,
                placeholder = placeholder?.let { { Text(placeholder) } },
                trailingIcon = trailingIcon?.let { { it(value, setValue) } }
            )
        }
    )
}