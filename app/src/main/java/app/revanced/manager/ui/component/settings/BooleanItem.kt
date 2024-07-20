package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.ui.component.SafeguardConfirmationDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BooleanItem(
    modifier: Modifier = Modifier,
    isSafeguard: Boolean = false,
    suggestedValue: Boolean = false,
    preference: Preference<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int,
    @StringRes confirmationText: Int = 0
) {
    val value by preference.getAsState()
    var showSafeguardWarning by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSafeguardWarning) {
        SafeguardConfirmationDialog(
            onDismiss = { showSafeguardWarning = false },
            onConfirm = {
                coroutineScope.launch { preference.update(!value) }
                showSafeguardWarning = false
            },
            body = stringResource(confirmationText)
        )
    }

    BooleanItem(
        modifier = modifier,
        value = value,
        onValueChange = {
            if (isSafeguard && it != suggestedValue) {
                showSafeguardWarning = true
            } else {
                coroutineScope.launch { preference.update(it) }
            }},
        headline = headline,
        description = description
    )
}

@Composable
fun BooleanItem(
    modifier: Modifier = Modifier,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @StringRes headline: Int,
    @StringRes description: Int
) = SettingsListItem(
    modifier = Modifier
        .clickable { onValueChange(!value) }
        .then(modifier),
    headlineContent = stringResource(headline),
    supportingContent = stringResource(description),
    trailingContent = {
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
)