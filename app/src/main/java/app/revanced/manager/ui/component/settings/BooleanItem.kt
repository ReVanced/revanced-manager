package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.ui.component.haptics.HapticSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BooleanItem(
    modifier: Modifier = Modifier,
    preference: Preference<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int
) {
    val value by preference.getAsState()

    BooleanItem(
        modifier = modifier,
        value = value,
        onValueChange = { coroutineScope.launch { preference.update(it) } },
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
        HapticSwitch(
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
)