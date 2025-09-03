package app.revanced.manager.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringDef
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.revanced.manager.domain.manager.PreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@StringDef(
    Manifest.permission.POST_NOTIFICATIONS
)
@Retention(AnnotationRetention.SOURCE)
annotation class AndroidPermission

class PermissionHelper : KoinComponent {
    private val context: Context by inject()
    private val prefs: PreferencesManager = get()

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun isFirstTimeAsking(permission: String): Boolean {
       with(prefs) {
           val firstTimeAsking = permission !in askedPermissions.get()
           if (firstTimeAsking) edit {
               askedPermissions += permission
           }
           return firstTimeAsking
       }
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    suspend fun getPermissionState(activity: Activity, permission: String): PermissionState {
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
