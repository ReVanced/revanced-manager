package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.util.transparentListItemColors

@Composable
fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: (() -> Unit)? = null,
    compatible: Boolean = true
) = ListItem(
    modifier = Modifier
        .let { if (!compatible) it.alpha(0.5f) else it }
        .then(
            if (onToggle != null) {
                Modifier.clickable(onClick = onToggle)
            } else Modifier
        )
        .fillMaxSize(),
    leadingContent = {
        if (onToggle != null) {
            HapticCheckbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = compatible
            )
        }
    },
    headlineContent = { Text(patch.name) },
    supportingContent = patch.description?.let { { Text(it) } },
    trailingContent = {
        if (patch.options?.isNotEmpty() == true) {
            IconButton(onClick = onOptionsDialog, enabled = compatible) {
                Icon(Icons.Outlined.Settings, null)
            }
        }
    },
    colors = transparentListItemColors
)

fun LazyListScope.patchList(
    patches: List<PatchInfo>,
    visible: Boolean,
    compatible: Boolean,
    onOptionsDialog: (PatchInfo) -> Unit,
    isSelected: (PatchInfo) -> Boolean,
    onPatchClick: (PatchInfo, Boolean) -> Unit,
    header: (@Composable () -> Unit)? = null
) {
    if (patches.isNotEmpty() && visible) {
        header?.let {
            item(contentType = 0) { it() }
        }

        items(
            items = patches,
            key = { it.name },
            contentType = { 1 }
        ) { patch ->
            PatchItem(
                patch = patch,
                onOptionsDialog = { onOptionsDialog(patch) },
                selected = compatible && isSelected(patch),
                onToggle = { onPatchClick(patch, compatible) },
                compatible = compatible
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.bundlePatchListReadOnly(
    patches: List<PatchInfo>,
    onOptionsDialog: ((PatchInfo) -> Unit)? = null
) {
    items(
        items = patches,
        contentType = { "readonly_patch" }
    ) { patch ->
        ListItem(
            modifier = Modifier.fillMaxWidth().animateItem(),
            headlineContent = { Text(patch.name) },
            supportingContent = {
                patch.description?.let { Text(it) }
                CompatibilityTags(patch)
            },
            trailingContent = if (onOptionsDialog != null && patch.options?.isNotEmpty() == true) {
                {
                    IconButton(onClick = { onOptionsDialog(patch) }) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                }
            } else null,
            colors = transparentListItemColors
        )
    }
}

@Composable
fun ListHeader(
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
                IconButton(onClick = it) {
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

@Composable
private fun PatchTag(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompatibilityTags(patch: PatchInfo) {
    val packages = patch.compatiblePackages

    FlowRow(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (packages != null) {
            packages.forEach { pkg ->
                var expanded by remember { mutableStateOf(false) }
                val containerColor = MaterialTheme.colorScheme.secondaryContainer
                val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

                PatchTag(
                    text = pkg.packageName,
                    containerColor = containerColor,
                    contentColor = contentColor
                )

                val versions = pkg.versions?.reversed()
                if (versions != null) {
                    val visibleVersions = if (expanded) versions else versions.take(1)
                    visibleVersions.forEach { version ->
                        PatchTag(
                            text = version,
                            containerColor = containerColor,
                            contentColor = contentColor
                        )
                    }
                    if (versions.size > 1) {
                        Surface(
                            modifier = Modifier.size(21.dp),
                            onClick = { expanded = !expanded },
                            shape = RoundedCornerShape(4.dp),
                            color = containerColor,
                            contentColor = contentColor,
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp,
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        } else {
            PatchTag(
                text = stringResource(R.string.universal),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
