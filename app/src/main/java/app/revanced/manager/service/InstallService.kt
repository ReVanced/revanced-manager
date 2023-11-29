package app.revanced.manager.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder

@Suppress("DEPRECATION")
class InstallService : Service() {

    override fun onStartCommand(
        intent: Intent, flags: Int, startId: Int
    ): Int {
        val extraStatus = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        val extraStatusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val extraPackageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        when (extraStatus) {
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
                    action = APP_INSTALL_ACTION
                    `package` = packageName
                    putExtra(EXTRA_INSTALL_STATUS, extraStatus)
                    putExtra(EXTRA_INSTALL_STATUS_MESSAGE, extraStatusMessage)
                    putExtra(EXTRA_PACKAGE_NAME, extraPackageName)
                })
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val APP_INSTALL_ACTION = "APP_INSTALL_ACTION"

        const val EXTRA_INSTALL_STATUS = "EXTRA_INSTALL_STATUS"
        const val EXTRA_INSTALL_STATUS_MESSAGE = "EXTRA_INSTALL_STATUS_MESSAGE"
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
    }

}