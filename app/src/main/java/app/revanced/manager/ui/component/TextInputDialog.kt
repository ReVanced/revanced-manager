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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.revanced.manager.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextInputDialog(
    initial: String,
    title: String,
    placeholder: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    validator: (String) -> Boolean = String::isNotEmpty,
    confirmValidator: (suspend (String) -> String?)? = null,
    trailingIcon: @Composable ((value: String, onValueChange: (String) -> Unit) -> Unit)? = null,
) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    var submitError by rememberSaveable(initial) { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val valid = remember(value, validator) {
        validator(value)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (confirmValidator == null) {
                        onConfirm(value)
                        return@TextButton
                    }

                    coroutineScope.launch {
                        isSubmitting = true
                        val validationError = confirmValidator(value)
                        isSubmitting = false

                        if (validationError == null) {
                            onConfirm(value)
                        } else {
                            submitError = validationError
                        }
                    }
                },
                enabled = valid && !isSubmitting,
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
                onValueChange = {
                    value = it
                    submitError = null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                ),
                placeholder = placeholder?.let { { Text(placeholder) } },
                trailingIcon = trailingIcon?.let { { it(value) { newValue ->
                    value = newValue
                    submitError = null
                } } },
                isError = submitError != null || (!valid && value.isNotEmpty()),
                supportingText = {
                    when {
                        submitError != null -> Text(
                            text = submitError!!,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                        !valid && value.isNotEmpty() -> {
                        Text(
                            text = stringResource(R.string.input_dialog_value_invalid),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                        }
                    }
                }
            )
        }
    )
}