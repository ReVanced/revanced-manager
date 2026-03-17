package app.revanced.manager.ui.component.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import app.revanced.manager.ui.component.TooltipIconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IntegerItem(
    modifier: Modifier = Modifier,
    preference: Preference<Int>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @StringRes headline: Int,
    @StringRes description: Int,
    unit: String? = null,
) {
    val value by preference.getAsState()

    IntegerItem(
        modifier = modifier,
        value = value,
        onValueChange = { coroutineScope.launch { preference.update(it) } },
        headline = headline,
        description = description,
        unit = unit
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntegerItem(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    @StringRes headline: Int,
    @StringRes description: Int,
    unit: String? = null,
) {
    var dialogOpen by rememberSaveable {
        mutableStateOf(false)
    }

    if (dialogOpen) {
        IntInputDialog(
            current = value,
            unit = unit,
            name = stringResource(headline)
        ) { new ->
            dialogOpen = false
            new?.let(onValueChange)
        }
    }

    SettingsListItem(
        modifier = modifier,
        headlineContent = stringResource(headline),
        supportingContent = stringResource(description),
        trailingContent = {
            TooltipIconButton(
                onClick = { dialogOpen = true },
                tooltip = stringResource(R.string.edit)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        },
        onClick = { dialogOpen = true }
    )
}