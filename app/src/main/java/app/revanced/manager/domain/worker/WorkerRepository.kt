package app.revanced.manager.domain.worker

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.revanced.manager.R
import app.revanced.manager.domain.manager.BackgroundBundleUpdateTime
import app.revanced.manager.patcher.worker.BundleUpdateNotificationWorker
import java.util.UUID
import java.util.concurrent.TimeUnit

class WorkerRepository(app: Application) {
    val workManager = WorkManager.getInstance(app)

    /**
     * The standard WorkManager communication APIs use [androidx.work.Data], which has too many limitations.
     * We can get around those limits by passing inputs using global variables instead.
     */
    val workerInputs = mutableMapOf<UUID, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <A : Any, W : Worker<A>> claimInput(worker: W): A {
        val data = workerInputs[worker.id] ?: throw IllegalStateException("Worker was not launched via WorkerRepository")
        workerInputs.remove(worker.id)

        return data as A
    }

    inline fun <reified W : Worker<A>, A : Any> launchExpedited(name: String, input: A): UUID {
        val request =
            OneTimeWorkRequest.Builder(W::class.java) // create Worker
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        workerInputs[request.id] = input
        workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        return request.id
    }

    inline fun <reified T> createNotification(
        context: Context,
        notificationChannel: NotificationChannel,
        title: String,
        description: String
    ): Pair<Notification, NotificationManager> {
        val notificationIntent = Intent(context, T::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        return Pair(
            Notification.Builder(context, notificationChannel.id)
                .setContentTitle(title)
                .setContentText(description)
                .setLargeIcon(Icon.createWithResource(context, R.drawable.ic_notification))
                .setSmallIcon(Icon.createWithResource(context, R.drawable.ic_notification))
                .setContentIntent(pendingIntent).build(),
            notificationManager
        )
    }

    fun scheduleBundleUpdateNotificationWork(bundleUpdateTime: BackgroundBundleUpdateTime) {
        val workId = "BundleUpdateNotificationWork"
        if(bundleUpdateTime == BackgroundBundleUpdateTime.NEVER) {
            workManager.cancelUniqueWork(workId)
            Log.d("WorkManager","Cancelled job with workId $workId.")
        } else {
            val workRequest =
                PeriodicWorkRequestBuilder<BundleUpdateNotificationWorker>(bundleUpdateTime.value, TimeUnit.MINUTES)
                    .build()

            workManager
                .enqueueUniquePeriodicWork(
                    workId,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    workRequest
                )
            Log.d("WorkManager", "Periodic work $workId updated with time ${bundleUpdateTime.value}.")
        }
    }
}