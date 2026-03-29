package app.revanced.manager.ui.component.selectedapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.component.ListSection

@Composable
fun BuildSectionLayouts(items: List<ListItem>) {
    val sections = mutableListOf<MutableList<ListItem>>()
    var current = mutableListOf<ListItem>()

    fun flush() {
        if (current.isNotEmpty()) {
            sections += current
            current = mutableListOf()
        }
    }

    items.forEach { item ->
        when (item) {
            is ListItem.Content -> {
                current += item
            }

            is ListItem.Info,
            is ListItem.Error -> {
                current += item

                flush()
            }
        }
    }

    flush()

    sections.forEach { section ->
        ListSection {
            section.forEach { item ->
                when (item) {
                    is ListItem.Content -> item.content()
                    is ListItem.Info -> item.content()
                    is ListItem.Error -> item.content()
                }
            }
        }
    }
}

@Composable
fun ErrorListItem(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    InfoListItem(
        text = text,
        actionText = actionText,
        onActionClick = onActionClick,
        compact = true,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        icon = Icons.Outlined.WarningAmber
    )
}

@Composable
fun InfoListItem(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    compact: Boolean? = false,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    icon: ImageVector = Icons.Outlined.Info,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        compact?.let {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
            return@Surface
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            if (actionText != null && onActionClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onActionClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = contentColor
                        )
                    ) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

sealed interface ListItem {
    data class Content(val content: @Composable () -> Unit) : ListItem
    data class Info(val content: @Composable () -> Unit) : ListItem
    data class Error(val content: @Composable () -> Unit) : ListItem
}

fun MutableList<ListItem>.item(content: @Composable () -> Unit) {
    add(ListItem.Content(content))
}

fun MutableList<ListItem>.info(content: @Composable () -> Unit) {
    add(ListItem.Info(content))
}

fun MutableList<ListItem>.error(content: @Composable () -> Unit) {
    add(ListItem.Error(content))
}