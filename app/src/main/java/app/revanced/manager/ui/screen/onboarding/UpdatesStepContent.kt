package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem

@Composable
fun UpdatesStepContent(
    managerEnabled: Boolean,
    patchesEnabled: Boolean,
    onManagerEnabledChange: (Boolean) -> Unit,
    onPatchesEnabledChange: (Boolean) -> Unit,
    showSubtitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    ColumnWithScrollbarEdgeShadow(modifier = modifier.fillMaxSize()) {
        if (showSubtitle) {
            Text(
                text = stringResource(R.string.onboarding_updates_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        ListSection {
            UpdatesItem(
                headline = stringResource(R.string.auto_updates_dialog_manager),
                icon = Icons.Outlined.Update,
                checked = managerEnabled,
                onCheckedChange = onManagerEnabledChange
            )
            UpdatesItem(
                headline = stringResource(R.string.auto_updates_dialog_patches),
                icon = Icons.Outlined.Source,
                checked = patchesEnabled,
                onCheckedChange = onPatchesEnabledChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.auto_updates_dialog_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun UpdatesItem(
    headline: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsListItem(
        onClick = { onCheckedChange(!checked) },
        headlineContent = headline,
        leadingContent = { OnboardingLeadingIcon(icon = icon) },
        trailingContent = {
            HapticCheckbox(
                checked = checked,
                onCheckedChange = null
            )
        }
    )
}
