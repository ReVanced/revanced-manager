package app.revanced.manager.ui.component.bundle

import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BundleListItem(
    modifier: Modifier = Modifier,
    headlineText: String,
    supportingText: String = "",
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        },
        trailingContent = trailingContent,
        modifier = modifier
    )
}