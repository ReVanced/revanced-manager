package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.settings.SettingsListItem

@Composable
fun UpdatesStepContent(
    managerEnabled: Boolean,
    patchesEnabled: Boolean,
    downloadersEnabled: Boolean,
    onManagerEnabledChange: (Boolean) -> Unit,
    onPatchesEnabledChange: (Boolean) -> Unit,
    onDownloadersEnabledChange: (Boolean) -> Unit,
) {
    ListSection(contentPadding = PaddingValues(0.dp)) {
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
        UpdatesItem(
            headline = stringResource(R.string.auto_updates_dialog_downloaders),
            icon = Icons.Outlined.Download,
            checked = downloadersEnabled,
            onCheckedChange = onDownloadersEnabledChange
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
