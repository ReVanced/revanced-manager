package app.revanced.manager.ui.component.patches

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.SafeguardDialog

@Composable
fun SelectionWarningDialog(
    onDismiss: () -> Unit
) {
    SafeguardDialog(
        onDismiss = onDismiss,
        title = R.string.warning,
        body = stringResource(R.string.selection_warning_description),
    )
}