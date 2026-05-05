package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.ui.component.ConfirmDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SafeguardBooleanItem(
    modifier: Modifier = Modifier,
    preference: Preference<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int,
    @StringRes dialogTitle: Int,
    @StringRes confirmationText: Int,
    onValueChange: ((Boolean) -> Unit)? = null
) = SafeguardBooleanItem(
    modifier = modifier,
    preference = preference,
    coroutineScope = coroutineScope,
    headline = headline,
    description = stringResource(description),
    dialogTitle = dialogTitle,
    confirmationText = confirmationText,
    onValueChange = onValueChange
)

@Composable
fun SafeguardBooleanItem(
    modifier: Modifier = Modifier,
    preference: Preference<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    description: String,
    @StringRes dialogTitle: Int,
    @StringRes confirmationText: Int,
    onValueChange: ((Boolean) -> Unit)? = null
) {
    val value by preference.getAsState()

    val update = onValueChange ?: { coroutineScope.launch { preference.update(it) } }

    SafeguardBooleanItem(
        modifier = modifier,
        value = value,
        default = preference.default,
        headline = headline,
        description = description,
        dialogTitle = dialogTitle,
        confirmationText = confirmationText,
        onValueChange = { update(it) }
    )
}

@Composable
fun SafeguardBooleanItem(
    modifier: Modifier = Modifier,
    value: Boolean,
    default: Boolean = false,
    @StringRes headline: Int,
    description: String,
    @StringRes dialogTitle: Int,
    @StringRes confirmationText: Int,
    onValueChange: (Boolean) -> Unit
) {
    var showSafeguardWarning by rememberSaveable { mutableStateOf(false) }

    if (showSafeguardWarning) {
        ConfirmDialog(
            onDismiss = { showSafeguardWarning = false },
            onConfirm = {
                onValueChange(!value)
                showSafeguardWarning = false
            },
            title = stringResource(id = dialogTitle),
            description = stringResource(confirmationText),
            icon = Icons.Outlined.WarningAmber
        )
    }

    BooleanItem(
        modifier = modifier,
        value = value,
        onValueChange = {
            if (it != default) {
                showSafeguardWarning = true
            } else {
                onValueChange(it)
            }
        },
        headline = headline,
        description = description
    )
}