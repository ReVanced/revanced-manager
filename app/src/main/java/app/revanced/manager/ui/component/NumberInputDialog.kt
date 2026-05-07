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
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private inline fun <T> NumberInputDialog(
    current: T?,
    name: String,
    unit: String?,
    crossinline onSubmit: (T?) -> Unit,
    crossinline validator: @DisallowComposableCalls (T) -> Boolean,
    crossinline toNumberOrNull: @DisallowComposableCalls String.() -> T?
) {
    var fieldValue by rememberSaveable {
        mutableStateOf(current?.toString().orEmpty())
    }
    val numberFieldValue by remember {
        derivedStateOf { fieldValue.toNumberOrNull() }
    }
    val validatorFailed by remember {
        derivedStateOf { numberFieldValue?.let { !validator(it) } ?: false }
    }

    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        title = { Text(name) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = {
                    Text(stringResource(R.string.dialog_input_placeholder))
                },
                isError = validatorFailed,
                suffix = unit?.let { { Text(it) } },
                supportingText = {
                    if (validatorFailed) {
                        Text(
                            stringResource(R.string.input_dialog_value_invalid),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { numberFieldValue?.let(onSubmit) },
                enabled = numberFieldValue != null && !validatorFailed,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun IntInputDialog(
    current: Int?,
    name: String,
    unit: String? = null,
    validator: (Int) -> Boolean = { true },
    onSubmit: (Int?) -> Unit
) = NumberInputDialog(current, name, unit, onSubmit, validator, String::toIntOrNull)

@Composable
fun LongInputDialog(
    current: Long?,
    name: String,
    unit: String? = null,
    validator: (Long) -> Boolean = { true },
    onSubmit: (Long?) -> Unit
) = NumberInputDialog(current, name, unit, onSubmit, validator, String::toLongOrNull)

@Composable
fun FloatInputDialog(
    current: Float?,
    name: String,
    unit: String? = null,
    validator: (Float) -> Boolean = { true },
    onSubmit: (Float?) -> Unit
) = NumberInputDialog(current, name, unit, onSubmit, validator, String::toFloatOrNull)