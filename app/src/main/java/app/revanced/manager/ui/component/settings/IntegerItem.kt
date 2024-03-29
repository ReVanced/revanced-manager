package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.base.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IntegerItem(
    modifier: Modifier = Modifier,
    preference: Preference<Int>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int
) {
    val value by preference.getAsState()

    IntegerItem(
        modifier = modifier,
        value = value,
        onValueChange = { coroutineScope.launch { preference.update(it) } },
        headline = headline,
        description = description
    )
}

@Composable
fun IntegerItem(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    @StringRes headline: Int,
    @StringRes description: Int
) {
    var dialogOpen by rememberSaveable {
        mutableStateOf(false)
    }

    if (dialogOpen) {
        IntegerItemDialog(current = value, name = headline) { new ->
            dialogOpen = false
            new?.let(onValueChange)
        }
    }

    SettingsListItem(
        modifier = Modifier
            .clickable { dialogOpen = true }
            .then(modifier),
        headlineContent = stringResource(headline),
        supportingContent = stringResource(description),
        trailingContent = {
            IconButton(onClick = { dialogOpen = true }) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    )
}

@Composable
private fun IntegerItemDialog(current: Int, @StringRes name: Int, onSubmit: (Int?) -> Unit) {
    var fieldValue by rememberSaveable {
        mutableStateOf(current.toString())
    }

    val integerFieldValue by remember {
        derivedStateOf {
            fieldValue.toIntOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        title = { Text(stringResource(name)) },
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
                onClick = { integerFieldValue?.let(onSubmit) },
                enabled = integerFieldValue != null,
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