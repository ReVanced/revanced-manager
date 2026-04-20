package app.revanced.manager.ui.component.patches

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatchesListHeader(
    title: String,
    onHelpClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        },
        trailingContent = onHelpClick?.let {
            {
                TooltipIconButton(onClick = it, tooltip = stringResource(R.string.help)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        stringResource(R.string.help)
                    )
                }
            }
        },
        colors = transparentListItemColors
    )
}