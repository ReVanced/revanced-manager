package app.revanced.manager.ui.component.patches

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScopeDialog(
    bundleName: String,
    onDismissRequest: () -> Unit,
    onAllPatches: () -> Unit,
    onBundleOnly: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.scope_dialog_title)) },
    confirmButton = {
        TextButton(onClick = onAllPatches, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.scope_all_patches))
        }
    },
    dismissButton = {
        TextButton(onClick = onBundleOnly, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.scope_bundle_patches, bundleName))
        }
    }
)