package app.revanced.manager.util.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class PermissionHelper(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences("permissions_pref", Context.MODE_PRIVATE)
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isFirstTimeAsking(permission: String): Boolean {
        val firstTime = prefs.getBoolean(permission, true)
        if (firstTime) {
            prefs.edit { putBoolean(permission, false) }
        }
        return firstTime
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun resetPermission(permission: String) {
        prefs.edit { putBoolean(permission, true) }
    }

    fun getPermissionState(activity: Activity, permission: String): PermissionState {
        return when {
            isPermissionGranted(permission) -> PermissionState.Granted
            shouldShowRationale(activity, permission) -> PermissionState.DeniedWithRationale
            isFirstTimeAsking(permission) -> PermissionState.FirstTime
            else -> PermissionState.DeniedPermanently
        }
    }

    enum class PermissionState {
        Granted,
        FirstTime,
        DeniedWithRationale,
        DeniedPermanently
    }
}
