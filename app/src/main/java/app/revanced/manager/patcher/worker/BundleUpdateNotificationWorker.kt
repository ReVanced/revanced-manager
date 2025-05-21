package app.revanced.manager.patcher.worker

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.WorkerParameters
import app.revanced.manager.MainActivity
import app.revanced.manager.R
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.worker.Worker
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.plugin.downloader.PluginHostApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(PluginHostApi::class)
class BundleUpdateNotificationWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker<BundleUpdateNotificationWorker.Args>(context, parameters), KoinComponent {
    private val patchBundleRepository: PatchBundleRepository by inject()
    private val workerRepository: WorkerRepository by inject()

    class Args()

    val notificationChannel = NotificationChannel(
        "background-bundle-update-channel", "Background Check Notifications", NotificationManager.IMPORTANCE_HIGH
    )
    companion object {
        const val LOG_TAG = "BundleAutoUpdateWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(LOG_TAG, "Searching for updates.")
        return try {
            val shouldSendNotification = patchBundleRepository.updateCheck()
            Log.d(LOG_TAG, "Found ${shouldSendNotification.size} new updates.")
            shouldSendNotification.forEach {
                it.getOrNull()?.let { bundle ->
                    sendNotification(bundle.getName(), bundle.currentVersion()!!)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Error during work: ${e.message}")
            Result.failure()
        }
    }

    private fun sendNotification(bundleName: String, bundleVersion: String) {
         workerRepository.createNotification<MainActivity>(
            applicationContext,
            notificationChannel,
            applicationContext.getString(R.string.bundle_update),
            applicationContext.getString(
                R.string.bundle_update_description,
                bundleName,
                bundleVersion
            )
        ).also { (notification, notificationManager) ->
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                 if (ContextCompat.checkSelfPermission(
                         applicationContext,
                         POST_NOTIFICATIONS
                     ) == PERMISSION_GRANTED
                 ) {
                     notificationManager.notify("$bundleName-$bundleVersion".hashCode(), notification)
                     Log.d(LOG_TAG, "Notification sent.")
                 } else {
                     Log.d(
                         LOG_TAG,
                         "POST_NOTIFICATIONS permission not granted. Cannot send notification."
                     )
                 }
             } else {
                 notificationManager.notify("$bundleName-$bundleVersion".hashCode(), notification)
                 Log.d(LOG_TAG, "Notification sent (pre-Android 13).")
             }
         }
    }
}