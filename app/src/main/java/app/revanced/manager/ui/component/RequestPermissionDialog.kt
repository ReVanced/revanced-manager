package app.revanced.manager.ui.component

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.util.permissions.PermissionHelper

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
    val activity = context as? Activity ?: return
    val permissionHelper = PermissionHelper(context)

    val launcher = rememberLauncherForActivityResult(contract) { result ->
        Log.d("testtt", "rememberLauncherForActivityResult $result")
        onResult(result)
    }

    when (permissionHelper.getPermissionState(activity, input)) {
        PermissionHelper.PermissionState.Granted -> {
            onResult(true)
        }
        PermissionHelper.PermissionState.FirstTime,
        PermissionHelper.PermissionState.DeniedWithRationale -> {
            PermissionDialog(
                title = title,
                description = description,
                icon = icon,
                onDismissRequest = onDismissRequest,
                onContinue = {
                    onContinue()
                    launcher.launch(input)
                }
            )
        }
        PermissionHelper.PermissionState.DeniedPermanently -> {
            onResult(false)
        }
    }
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