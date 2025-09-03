package app.revanced.manager.util.permissions

import android.Manifest
import android.content.Context
import android.os.Build

fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        PermissionHelper().isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    else
        true
}
