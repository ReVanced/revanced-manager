package app.revanced.manager.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder

@Suppress("DEPRECATION")
class UninstallService : Service() {

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int
    ): Int {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                startActivity(if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }.apply {
                    this?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }

            else -> {
                sendBroadcast(Intent().apply {
                    action = APP_UNINSTALL_ACTION
                })
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val APP_UNINSTALL_ACTION = "APP_UNINSTALL_ACTION"
    }

}