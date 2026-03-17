package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
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
) = BooleanItem(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    headline = headline,
    description = stringResource(description)
)

@Composable
fun BooleanItem(
    modifier: Modifier = Modifier,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @StringRes headline: Int,
    description: String
) = SettingsListItem(
    modifier = modifier,
    headlineContent = stringResource(headline),
    supportingContent = description,
    onClick = { onValueChange(!value) },
    trailingContent = {
        HapticSwitch(
            checked = value,
            onCheckedChange = onValueChange,
            thumbContent = if (value) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        )
    }
)