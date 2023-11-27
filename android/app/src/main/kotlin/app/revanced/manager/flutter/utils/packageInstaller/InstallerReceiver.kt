package app.revanced.manager.flutter.utils.packageInstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import app.revanced.manager.flutter.MainActivity

class InstallerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmationIntent != null) {
                    context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            else -> {
                val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
                MainActivity.PackageInstallerManager.result!!.success(mapOf(
                    "status" to status,
                    "packageName" to packageName,
                    "message" to message,
                    "otherPackageName" to otherPackageName
                ))
            }
        }
    }
}