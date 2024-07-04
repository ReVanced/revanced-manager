package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

@Composable
fun NotificationCard(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    title: String? = null,
    isWarning: Boolean = false
) {
    val color =
        if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer

    NotificationCardInstance(modifier = modifier, isWarning = isWarning) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    actions?.invoke(this)
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    title: String? = null,
    isWarning: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val color =
        if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer

    NotificationCardInstance(modifier = modifier, isWarning = isWarning, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                )
            }
            if (title != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            } else {
                Text(
                    modifier = Modifier.weight(1f),
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCardInstance(
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors =
        CardDefaults.cardColors(containerColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer)
    val defaultModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))

    if (onClick != null) {
        Card(
            onClick = onClick,
            colors = colors,
            modifier = modifier.then(defaultModifier)
        ) {
            content()
        }
    } else {
        Card(
            colors = colors,
            modifier = modifier.then(defaultModifier)
        ) {
            content()
        }
    }
}