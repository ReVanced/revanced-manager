package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.ui.component.IntInputDialog
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
        IntInputDialog(current = value, name = stringResource(headline)) { new ->
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