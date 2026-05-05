package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
            // We check if the value is empty to avoid showing an error state when the user
            // hasn't entered anything yet to avoid bashing user with bad UX of blaming the
            // user immediately when everything is intended.
            OutlinedTextField(
                value = value,
                onValueChange = setValue,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                ),
                placeholder = placeholder?.let { { Text(placeholder) } },
                trailingIcon = trailingIcon?.let { { it(value, setValue) } },
                isError = !valid && value.isNotEmpty(),
                supportingText = {
                    if (!valid && value.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.input_dialog_value_invalid),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    )
}