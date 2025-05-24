package app.revanced.manager.patcher.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import app.revanced.manager.MainActivity
import app.revanced.manager.R
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.worker.Worker
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.util.hasNotificationPermission
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
        /**
         * If the user did not consent to be notified, there is no point in checking in background.
         * The auto update will still happen on app opening.
         **/
        if (!hasNotificationPermission(applicationContext))
            return Result.success()

        Log.d(LOG_TAG, "Searching for updates.")
        return try {
            patchBundleRepository.updateCheck().forEach {
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
             if (hasNotificationPermission(applicationContext))
                 notificationManager.notify(
                     "$bundleName-$bundleVersion".hashCode(),
                     notification
                 )
         }
    }
}