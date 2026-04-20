package app.revanced.manager.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordField(modifier: Modifier = Modifier, value: String, onValueChange: (String) -> Unit, label: @Composable (() -> Unit)? = null, placeholder: @Composable (() -> Unit)? = null) {
    var visible by rememberSaveable {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        label = label,
        modifier = modifier,
        trailingIcon = {
            val (icon, description) = remember(visible) {
                if (visible) Icons.Outlined.VisibilityOff to R.string.hide_password_field else Icons.Outlined.Visibility to R.string.show_password_field
            }
            TooltipIconButton(
                onClick = { visible = !visible },
                tooltip = stringResource(description)
            ) {
                Icon(icon, stringResource(description))
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    )
}