package app.revanced.manager.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Credits to [Vendetta](https://github.com/vendetta-mod)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.SegmentedButton(
    icon: Any,
    text: String,
    onClick: () -> Unit,
    iconDescription: String? = null,
    enabled: Boolean = true
) {
    val contentColor = if (enabled)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface.copy(0.38f)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .background(
                    if (enabled)
                        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(0.12f)
                )
                .weight(1f)
                .padding(vertical = 20.dp)
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconDescription
                    )
                }

                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = iconDescription
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}