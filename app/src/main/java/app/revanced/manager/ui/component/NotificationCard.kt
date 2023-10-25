package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
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
    isWarning: Boolean = false,
    title: String? = null,
    text: String,
    icon: ImageVector,
    onDismiss: (() -> Unit)? = null,
    tertiaryAction: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = (if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (actions != null) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (actions == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                    )
                    if (title == null) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row {
                        tertiaryAction?.invoke()
                        if (onDismiss != null) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                if (title == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = icon,
                            contentDescription = null
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = icon,
                        contentDescription = null
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        tertiaryAction?.invoke()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onDismiss != null) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = stringResource(id = R.string.dismiss),
                                    color = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        actions.invoke()
                    }
                }
            }
        }
    }
}