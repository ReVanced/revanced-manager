package app.revanced.manager.domain.worker

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import app.revanced.manager.R
import java.util.UUID

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

    fun createNotification(
        context: Context,
        notificationChannel: NotificationChannel,
        pendingIntent: PendingIntent,
        title: String,
        description: String,
        autoCancel: Boolean = true
    ): Pair<Notification, NotificationManager> {
        val notificationManager = (context
            .getSystemService(NotificationManager::class.java) as NotificationManager)
            .also { it.createNotificationChannel(notificationChannel) }

        return Notification.Builder(context, notificationChannel.id)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(Icon.createWithResource(context, R.drawable.ic_notification))
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setAutoCancel(autoCancel)
            .build() to notificationManager
    }
}
