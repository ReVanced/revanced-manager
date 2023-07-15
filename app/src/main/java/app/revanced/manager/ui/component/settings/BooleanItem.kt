package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.domain.manager.base.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BooleanItem(
    preference: Preference<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int
) {
    val value by preference.getAsState()

    BooleanItem(
        value = value,
        onValueChange = { coroutineScope.launch { preference.update(it) } },
        headline = headline,
        description = description
    )
}

@Composable
fun BooleanItem(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @StringRes headline: Int,
    @StringRes description: Int
) = ListItem(
    modifier = Modifier.clickable { onValueChange(!value) },
    headlineContent = { Text(stringResource(headline)) },
    supportingContent = { Text(stringResource(description)) },
    trailingContent = {
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
)