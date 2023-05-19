package app.revanced.manager.compose.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import app.revanced.manager.compose.patcher.patch.PatchInfo

@Composable
fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    supported: Boolean
) {
    ListItem(
        modifier = Modifier
            .let { if (!supported) it.alpha(0.5f) else it }
            .clickable(enabled = supported, onClick = onToggle),
        leadingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = {
                    onToggle()
                },
                enabled = supported
            )
        },
        headlineContent = {
            Text(patch.name)
        },
        supportingContent = {
            Text(patch.description ?: "")
        },
        trailingContent = {
            if (patch.options?.isNotEmpty() == true) {
                IconButton(onClick = onOptionsDialog, enabled = supported) {
                    Icon(Icons.Outlined.Settings, null)
                }
            }
        }
    )
}