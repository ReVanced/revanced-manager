package app.revanced.manager.ui.component

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.ui.viewmodel.PermissionViewModel

@Composable
fun PermissionRequestDialog(
    vm: PermissionViewModel,
    title: String,
    description: String,
    icon: ImageVector
) {
    val activity = LocalActivity.current!!
    val shouldShowDialog by vm.shouldShowDialog.collectAsStateWithLifecycle(false)

    LaunchedEffect(Unit) {
        vm.refreshPermissionState(activity)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        vm.refreshPermissionState(activity)
    }

    if (shouldShowDialog)
        ConfirmDialog(
            title = title,
            description = description,
            icon = icon,
            onDismiss = { vm.refreshPermissionState(activity) },
            onConfirm = { launcher.launch(vm.permission) }
        )
}
