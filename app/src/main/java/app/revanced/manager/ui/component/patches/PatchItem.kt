package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.util.transparentListItemColors
import kotlin.collections.firstOrNull
import kotlin.collections.map

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatchItem(
    patch: PatchInfo,
    onOptionsDialog: () -> Unit,
    selected: Boolean,
    onToggle: () -> Unit,
    compatible: Boolean = true,
    readOnly: Boolean = false,
    scopedPackageName: String? = null
) {
    val anyVersionLabel = stringResource(R.string.patches_view_any_version)
    val anyAppLabel = stringResource(R.string.universal)

    val chipLabels = remember(patch.compatiblePackages, scopedPackageName, anyVersionLabel, anyAppLabel) {
        val pkgs = patch.compatiblePackages
        when {
            pkgs == null -> if (scopedPackageName == null) listOf(anyAppLabel) else emptyList()
            scopedPackageName != null -> {
                val pkg = pkgs.firstOrNull { it.packageName == scopedPackageName }
                    ?: return@remember emptyList()
                val versions = pkg.versions
                if (versions.isNullOrEmpty()) listOf(anyVersionLabel) else versions.toList()
            }
            else -> {
                pkgs.map { pkg ->
                    val versions = pkg.versions
                    if (versions.isNullOrEmpty()) {
                        "${pkg.packageName} ($anyVersionLabel)"
                    } else {
                        "${pkg.packageName} (${versions.joinToString(", ")})"
                    }
                }
            }
        }
    }

    ListItem(
        modifier = Modifier
            .let { if (!compatible) it.alpha(0.5f) else it }
            .clickable(enabled = !readOnly, onClick = onToggle)
            .fillMaxSize(),
        leadingContent = {
            HapticCheckbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = compatible && !readOnly
            )
        },
        headlineContent = { Text(patch.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                patch.description?.let { Text(it) }
                if (chipLabels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chipLabels.forEach { label ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        trailingContent = {
            if (patch.options?.isNotEmpty() == true) {
                TooltipIconButton(
                    onClick = onOptionsDialog,
                    enabled = compatible || readOnly,
                    tooltip = stringResource(R.string.settings)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        },
        colors = transparentListItemColors
    )
}
