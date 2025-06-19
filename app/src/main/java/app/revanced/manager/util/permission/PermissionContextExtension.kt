package app.revanced.manager.util.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import app.revanced.manager.util.permission.PermissionHelper.PermissionState

fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        PermissionHelper(this).isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    else
        true
}

fun Context.isNotificationPermissionDenied(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        PermissionHelper(this).getPermissionState(this as Activity, Manifest.permission.POST_NOTIFICATIONS) ==
                PermissionState.DeniedPermanently
    else
        false
}
