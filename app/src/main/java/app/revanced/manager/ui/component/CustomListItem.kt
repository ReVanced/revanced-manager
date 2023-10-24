package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ListItem

@Composable
fun CustomListItem(
    headlineContent: String,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) = ListItem(
    headlineContent = {
        Text(
            text = headlineContent,
            style = MaterialTheme.typography.titleLarge
        )
    },
    modifier = modifier.then(Modifier.padding(horizontal = 8.dp)),
    overlineContent = overlineContent,
    supportingContent = {
        if (supportingContent != null)
            Text(
                text = supportingContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            ) else null
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    colors = colors,
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation
)