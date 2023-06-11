package app.revanced.manager.patcher.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.revanced.manager.R
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.tag
import app.revanced.patcher.extensions.PatchExtensions.patchName
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException

class PatcherWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters),
    KoinComponent {
    private val sourceRepository: SourceRepository by inject()

    @Serializable
    data class Args(
        val input: String,
        val output: String,
        val selectedPatches: PatchesSelection,
        val packageName: String,
        val packageVersion: String
    )

    companion object {
        const val ARGS_KEY = "args"
        private const val logPrefix = "[Worker]:"
        private fun String.logFmt() = "$logPrefix $this"
    }

    override suspend fun getForegroundInfo() = ForegroundInfo(1, createNotification())

    private fun createNotification(): Notification {
        val notificationIntent = Intent(applicationContext, PatcherWorker::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(
            "revanced-patcher-patching", "Patching", NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
        return Notification.Builder(applicationContext, channel.id)
            .setContentTitle(applicationContext.getText(R.string.app_name))
            .setContentText(applicationContext.getText(R.string.patcher_notification_message))
            .setLargeIcon(Icon.createWithResource(applicationContext, R.drawable.ic_notification))
            .setSmallIcon(Icon.createWithResource(applicationContext, R.drawable.ic_notification))
            .setContentIntent(pendingIntent).build()
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            Log.d(tag, "Android requested retrying but retrying is disabled.".logFmt())
            return Result.failure()
        }

        val args = Json.decodeFromString<Args>(inputData.getString(ARGS_KEY)!!)

        try {
            // This does not always show up for some reason.
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.d(tag, "Failed to set foreground info:", e)
        }

        val wakeLock: PowerManager.WakeLock =
            (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, "$tag::Patcher").apply {
                    acquire(10 * 60 * 1000L)
                    Log.d(tag, "Acquired wakelock.")
                }
            }

        return try {
            runPatcher(args)
        } finally {
            wakeLock.release()
        }
    }

    private suspend fun runPatcher(args: Args): Result {
        val aaptPath =
            Aapt.binary(applicationContext)?.absolutePath
                ?: throw FileNotFoundException("Could not resolve aapt.")

        val frameworkPath = applicationContext.cacheDir.resolve("framework").also { it.mkdirs() }.absolutePath

        val bundles = sourceRepository.bundles.first()
        val integrations = bundles.mapNotNull { (_, bundle) -> bundle.integrations }

        val patchList = args.selectedPatches.flatMap { (bundleName, selected) ->
            bundles[bundleName]?.loadPatchesFiltered(args.packageName)
                ?.filter { selected.contains(it.patchName) }
                ?: throw IllegalArgumentException("Patch bundle $bundleName does not exist")
        }

        val progressManager =
            PatcherProgressManager(applicationContext, patchList.map { it.patchName })

        suspend fun updateProgress(progress: Progress) {
            progressManager.handle(progress)
            setProgress(progressManager.groupsToWorkData())
        }

        updateProgress(Progress.Unpacking)

        return try {
            Session(applicationContext.cacheDir.absolutePath, frameworkPath, aaptPath, File(args.input)) {
                updateProgress(it)
            }.use { session ->
                session.run(File(args.output), patchList, integrations)
            }

            Log.i(tag, "Patching succeeded".logFmt())
            progressManager.success()
            Result.success(progressManager.groupsToWorkData())
        } catch (e: Exception) {
            Log.e(tag, "Got exception while patching".logFmt(), e)
            progressManager.failure()
            Result.failure(progressManager.groupsToWorkData())
        }
    }
}