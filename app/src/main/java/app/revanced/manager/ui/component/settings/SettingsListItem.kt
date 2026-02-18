package app.revanced.manager.ui.component.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsListItem(
    headlineContent: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )

    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        overlineContent = overlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent?.let {
            {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    trailingContent()
                }
            }
        },
        supportingContent = supportingContent?.let { { Text(it) } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(headlineContent)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )

    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        overlineContent = overlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent?.let { { Text(it) } },
        content = headlineContent
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableSettingsListItem(
    headlineContent: String,
    supportingContent: String,
    expandableContent: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsListItem(
            modifier = Modifier.semantics {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            onClick = { expanded = !expanded },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ListItemDefaults.SegmentedGap),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                expandableContent()
            }
        }
    }
}
