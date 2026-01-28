package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BundleListItem(
    modifier: Modifier = Modifier,
    headlineText: String,
    supportingText: String = "",
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )

    SegmentedListItem(
        onClick = onClick ?: {},
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        trailingContent = trailingContent,
        supportingContent = if (supportingText.isNotEmpty()) { { Text(supportingText) } } else null,
    ) {
        Text(headlineText)
    }
}