package app.revanced.manager.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import app.revanced.manager.util.permissions.PermissionHelper.PermissionState

fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        PermissionHelper(this).isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    else
        true
}

fun Context.shouldAskNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        !PermissionHelper(this).isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS) &&
                PermissionHelper(this).getPermissionState(
                    this as Activity, Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionState.DeniedPermanently
    else
        false
}