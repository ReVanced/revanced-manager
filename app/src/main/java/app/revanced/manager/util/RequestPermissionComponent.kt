package app.revanced.manager.util

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import app.revanced.manager.R
import app.revanced.manager.ui.component.AlertDialogExtended

@Composable
fun PermissionRequestHandler(
    contract: ActivityResultContract<String, Boolean>,
    input: String,
    onDismissRequest: () -> Unit,
    onResult: (Boolean) -> Unit,
    onContinue: () -> Unit = {},
    title: String,
    description: String,
    icon: ImageVector
) {
    val context = LocalContext.current
    val showDialog = ActivityCompat.shouldShowRequestPermissionRationale(
        context as Activity,
        input
    )

    val launcher = rememberLauncherForActivityResult(contract) { result ->
        onResult(result)
    }

    if(showDialog)
        PermissionDialog(
            title,
            description,
            icon,
            onDismissRequest = onDismissRequest,
            onContinue = {
                onContinue()
                launcher.launch(input)
            }
        )
    else
        onResult(false)
}

@Composable
private fun PermissionDialog(
    title: String,
    description: String,
    icon: ImageVector,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(description) },
        icon = { Icon(icon, null) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
