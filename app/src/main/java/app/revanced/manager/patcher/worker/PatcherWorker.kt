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
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.worker.Worker
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.tag
import app.revanced.patcher.extensions.PatchExtensions.options
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.logging.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException

class PatcherWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker<PatcherWorker.Args>(context, parameters), KoinComponent {

    private val patchBundleRepository: PatchBundleRepository by inject()
    private val workerRepository: WorkerRepository by inject()
    private val prefs: PreferencesManager by inject()
    private val downloadedAppRepository: DownloadedAppRepository by inject()
    private val pm: PM by inject()

    data class Args(
        val input: SelectedApp,
        val output: String,
        val selectedPatches: PatchesSelection,
        val options: Options,
        val packageName: String,
        val packageVersion: String,
        val progress: MutableStateFlow<ImmutableList<Step>>,
        val logger: Logger
    )

    companion object {
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

        val args = workerRepository.claimInput(this)

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

        val frameworkPath =
            applicationContext.cacheDir.resolve("framework").also { it.mkdirs() }.absolutePath

        val bundles = patchBundleRepository.bundles.first()
        val integrations = bundles.mapNotNull { (_, bundle) -> bundle.integrations }

        val downloadProgress = MutableStateFlow<Pair<Float, Float>?>(null)

        val progressManager =
            PatcherProgressManager(
                applicationContext,
                args.selectedPatches.flatMap { it.value },
                args.input,
                downloadProgress
            )

        val progressFlow = args.progress

        fun updateProgress(advanceCounter: Boolean = true) {
            if (advanceCounter) {
                progressManager.success()
            }
            progressFlow.value = progressManager.getProgress().toImmutableList()
        }

        return try {
            // TODO: consider passing all the classes directly now that the input no longer needs to be serializable.
            val selectedBundles = args.selectedPatches.keys
            val allPatches = bundles.filterKeys { selectedBundles.contains(it) }
                .mapValues { (_, bundle) -> bundle.patchClasses(args.packageName) }

            // Set all patch options.
            args.options.forEach { (bundle, configuredPatchOptions) ->
                val patches = allPatches[bundle] ?: return@forEach
                configuredPatchOptions.forEach { (patchName, options) ->
                    patches.single { it.patchName == patchName }.options?.let {
                        options.forEach { (key, value) ->
                            it[key] = value
                        }
                    }
                }
            }

            val patches = args.selectedPatches.flatMap { (bundle, selected) ->
                allPatches[bundle]?.filter { selected.contains(it.patchName) }
                    ?: throw IllegalArgumentException("Patch bundle $bundle does not exist")
            }


            // Ensure they are in the correct order so we can track progress properly.
            progressManager.replacePatchesList(patches.map { it.patchName })
            updateProgress() // Loading patches

            val inputFile = when (val selectedApp = args.input) {
                is SelectedApp.Download -> {
                    val savePath = applicationContext.filesDir.resolve("downloaded-apps")
                        .resolve(args.input.packageName).also { it.mkdirs() }

                    selectedApp.app.download(
                        savePath,
                        prefs.preferSplits.get(),
                        onDownload = { downloadProgress.emit(it) }
                    ).also {
                        downloadedAppRepository.add(
                            args.input.packageName,
                            args.input.version,
                            it
                        )
                        updateProgress() // Downloading
                    }
                }

                is SelectedApp.Local -> selectedApp.file
                is SelectedApp.Installed -> File(pm.getPackageInfo(selectedApp.packageName)!!.applicationInfo.sourceDir)
            }

            Session(
                applicationContext.cacheDir.absolutePath,
                frameworkPath,
                aaptPath,
                args.logger,
                inputFile,
                onStepSucceeded = ::updateProgress
            ).use { session ->
                session.run(File(args.output), patches, integrations)
            }

            Log.i(tag, "Patching succeeded".logFmt())
            progressManager.success()
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Exception while patching".logFmt(), e)
            progressManager.failure(e)
            Result.failure()
        } finally {
            updateProgress(false)
        }
    }
}