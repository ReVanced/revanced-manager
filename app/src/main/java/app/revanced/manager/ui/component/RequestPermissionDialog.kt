package app.revanced.manager.ui.component

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.ui.viewmodel.PermissionStateHolder

@Composable
fun PermissionRequestDialog(
    stateHolder: PermissionStateHolder,
    contract: ActivityResultContract<String, Boolean>,
    title: String,
    description: String,
    icon: ImageVector,
    shouldShowDialogOverride: (() -> Boolean)? = null,
    onDismiss: () -> Unit
) {
    val activity = LocalActivity.current!!
    val permissionState by stateHolder.permissionState.collectAsStateWithLifecycle(null)

    val shouldShowDialog by remember {
        derivedStateOf {
            (shouldShowDialogOverride?.invoke()) ?:
                (permissionState?.shouldShowDialog(activity, stateHolder.permission) == true)
        }
    }

    val launcher = rememberLauncherForActivityResult(contract) {
        stateHolder.refreshPermissionState()
    }

    if (shouldShowDialog)
        ConfirmDialog(
            title = title,
            description = description,
            icon = icon,
            onDismiss = {
                stateHolder.refreshPermissionState()
                onDismiss()
            },
            onConfirm = { launcher.launch(stateHolder.permission) }
        )
}
