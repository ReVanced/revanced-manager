package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

@Composable
fun BundleInfoContent(
    switchChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    patchInfoText: String,
    patchCount: Int,
    onArrowClick: () -> Unit,
    isLocal: Boolean,
    tonalButtonOnClick: () -> Unit = {},
    tonalButtonContent: @Composable RowScope.() -> Unit,
) {
    if(!isLocal) {
        BundleInfoListItem(
            headlineText = stringResource(R.string.automatically_update),
            supportingText = stringResource(R.string.automatically_update_description),
            trailingContent = {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onCheckedChange
                )
            }
        )
    }

    BundleInfoListItem(
        headlineText = stringResource(R.string.bundle_type),
        supportingText = stringResource(R.string.bundle_type_description)
    ) {
        FilledTonalButton(
            onClick = tonalButtonOnClick,
            content = tonalButtonContent,
        )
    }

    Text(
        text = stringResource(R.string.information),
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    BundleInfoListItem(
        headlineText = stringResource(R.string.patches),
        supportingText = patchInfoText,
        trailingContent = {
            if (patchCount > 0) {
                IconButton(onClick = onArrowClick) {
                    Icon(
                        Icons.Outlined.ArrowRight,
                        stringResource(R.string.patches)
                    )
                }
            }
        }
    )

    BundleInfoListItem(
        headlineText = stringResource(R.string.patches_version),
        supportingText = "1.0.0",
    )

    BundleInfoListItem(
        headlineText = stringResource(R.string.integrations_version),
        supportingText = "1.0.0",
    )
}