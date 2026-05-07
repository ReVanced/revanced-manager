package app.revanced.manager.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import app.revanced.manager.util.withHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null
) {
    // Override default Material3 minimum touch target size to nothing
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FilterChip(
            selected = selected,
            onClick = onClick.withHapticFeedback(HapticFeedbackConstantsCompat.CONFIRM),
            label = label,
            modifier = modifier.height(32.dp),
            enabled = enabled,
            leadingIcon = {
                AnimatedVisibility(
                    visible = selected,
                    enter = expandIn(expandFrom = Alignment.CenterStart),
                    exit = shrinkOut(shrinkTowards = Alignment.CenterStart)
                ) {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        imageVector = Icons.Filled.Done,
                        contentDescription = null,
                    )
                }
            },
            trailingIcon = trailingIcon,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            interactionSource = interactionSource
        )
    }
}