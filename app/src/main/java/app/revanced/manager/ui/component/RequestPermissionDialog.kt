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
    vm: PermissionStateHolder,
    contract: ActivityResultContract<String, Boolean>,
    title: String,
    description: String,
    icon: ImageVector
) {
    val activity = LocalActivity.current!!
    val permissionState by vm.permissionState.collectAsStateWithLifecycle(null)

    val shouldShowDialog by remember {
        derivedStateOf {
            permissionState?.shouldShowDialog(activity, vm.permission) == true
        }
    }

    val launcher = rememberLauncherForActivityResult(contract) {
        vm.refreshPermissionState()
    }

    if (shouldShowDialog)
        ConfirmDialog(
            title = title,
            description = description,
            icon = icon,
            onDismiss = vm::refreshPermissionState,
            onConfirm = { launcher.launch(vm.permission) }
        )
}
