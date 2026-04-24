package app.revanced.manager.patcher.worker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Parcelable
import android.os.PowerManager
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.revanced.manager.MainActivity
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.worker.Worker
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.patcher.ProgressEvent
import app.revanced.manager.patcher.StepId
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.runStep
import app.revanced.manager.patcher.runtime.CoroutineRuntime
import app.revanced.manager.patcher.runtime.ProcessRuntime
import app.revanced.manager.patcher.toRemoteError
import app.revanced.manager.downloader.GetScope
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.Scope
import app.revanced.manager.downloader.UserInteractionException
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.tag
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException

@OptIn(DownloaderHostApi::class)
class PatcherWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker<PatcherWorker.Args>(context, parameters), KoinComponent {
    private val workerRepository: WorkerRepository by inject()
    private val prefs: PreferencesManager by inject()
    private val keystoreManager: KeystoreManager by inject()
    private val downloaderRepository: DownloaderRepository by inject()
    private val downloadedAppRepository: DownloadedAppRepository by inject()
    private val pm: PM by inject()
    private val fs: Filesystem by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val rootInstaller: RootInstaller by inject()

    class Args(
        val input: SelectedApp,
        val output: String,
        val selectedPatches: PatchSelection,
        val options: Options,
        val logger: Logger,
        val handleStartActivityRequest: suspend (LoadedDownloader, Intent) -> ActivityResult,
        val setInputFile: suspend (File) -> Unit,
        val onEvent: (ProgressEvent) -> Unit,
    ) {
        val packageName get() = input.packageName
    }

    override suspend fun getForegroundInfo() =
        ForegroundInfo(
            1,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

    private fun createNotification(): Notification {
        val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(
            "revanced-patcher-patching", "Patching", NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        return Notification.Builder(applicationContext, channel.id)
            .setContentTitle(applicationContext.getText(R.string.patcher_notification_title))
            .setContentText(applicationContext.getText(R.string.patcher_notification_text))
            .setSmallIcon(Icon.createWithResource(applicationContext, R.drawable.ic_notification))
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            Log.d(tag, "Android requested retrying but retrying is disabled.".logFmt())
            return Result.failure()
        }

        try {
            // This does not always show up for some reason.
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                throw e
            }
            Log.w(tag, "Failed to set foreground info", e)
        }
        @SuppressLint("WakelockTimeout")
        val wakeLock: PowerManager.WakeLock =
            (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag::Patcher")
                .apply {
                    // acquire without a timeout as we're managing the wakelock ourselves
                    // in the finally-block, and therefore, it will be released regardless (of failure/success eventually).
                    acquire()
                    Log.d(tag, "Acquired wakelock.")
                }

        val args = workerRepository.claimInput(this)

        return try {
            runPatcher(args)
        } finally {
            wakeLock.release()
        }
    }

    private suspend fun runPatcher(args: Args): Result {
        val patchedApk = fs.tempDir.resolve("patched.apk")
        var success = false

        return try {
            if (args.input is SelectedApp.Installed) {
                installedAppRepository.get(args.packageName)?.let {
                    if (it.installType == InstallType.MOUNT) {
                        rootInstaller.unmount(args.packageName)
                    }
                }
            }

            suspend fun download(downloader: LoadedDownloader, data: Parcelable) =
                downloadedAppRepository.download(
                    downloader,
                    data,
                    args.packageName,
                    args.input.version,
                    prefs.suggestedVersionSafeguard.get(),
                    !prefs.disablePatchVersionCompatCheck.get(),
                    onDownload = { progress ->
                        args.onEvent(
                            ProgressEvent.Progress(
                                stepId = StepId.DownloadAPK,
                                current = progress.first,
                                total = progress.second
                            )
                        )
                    }
                ).also { args.setInputFile(it) }

            val inputFile = when (val selectedApp = args.input) {
                is SelectedApp.Download -> {
                    runStep(StepId.DownloadAPK, args.onEvent) {
                        val (downloader, data) = downloaderRepository.unwrapParceledData(
                            selectedApp.data
                        )

                        download(downloader, data)
                    }
                }

                is SelectedApp.Search -> {
                    runStep(StepId.DownloadAPK, args.onEvent) {
                        downloaderRepository.loadedDownloadersFlow.first()
                            .firstNotNullOfOrNull { downloader ->
                                try {
                                    val getScope = object : GetScope, Scope by downloader.scopeImpl {
                                        override suspend fun requestStartActivity(intent: Intent): Intent? {
                                            val result =
                                                args.handleStartActivityRequest(downloader, intent)
                                            return when (result.resultCode) {
                                                Activity.RESULT_OK -> result.data
                                                Activity.RESULT_CANCELED -> throw UserInteractionException.Activity.Cancelled()
                                                else -> throw UserInteractionException.Activity.NotCompleted(
                                                    result.resultCode,
                                                    result.data
                                                )
                                            }
                                        }
                                    }
                                    withContext(Dispatchers.IO) {
                                        downloader.impl.get(
                                            getScope,
                                            selectedApp.packageName,
                                            selectedApp.version
                                        )
                                    }?.takeIf { (_, version) -> selectedApp.version == null || version == selectedApp.version }
                                } catch (e: UserInteractionException.Activity.NotCompleted) {
                                    throw e
                                } catch (e: UserInteractionException) {
                                    Log.i(tag, "User interaction cancelled downloader flow", e)
                                    throw e
                                } catch (e: IOException) {
                                    Log.w(
                                        tag,
                                        "Downloader ${downloader.name} failed with an IO error, trying next downloader",
                                        e
                                    )
                                    null
                                }?.let { (data, _) -> download(downloader, data) }
                            } ?: throw Exception("App is not available.")
                    }
                }

                is SelectedApp.Local -> selectedApp.file.also { args.setInputFile(it) }
                is SelectedApp.Installed -> {
                    val pkgInfo = pm.getPackageInfo(selectedApp.packageName)
                        ?: throw IllegalStateException("Package ${selectedApp.packageName} is not installed.")
                    val appInfo = pkgInfo.applicationInfo
                        ?: throw IllegalStateException("Failed to retrieve application info for ${selectedApp.packageName}.")
                    File(appInfo.sourceDir)
                }
            }

            val runtime = if (prefs.useProcessRuntime.get()) {
                ProcessRuntime(applicationContext)
            } else {
                CoroutineRuntime(applicationContext)
            }

            runtime.execute(
                inputFile.absolutePath,
                patchedApk.absolutePath,
                args.packageName,
                args.selectedPatches,
                args.options,
                args.logger,
                args.onEvent,
            )

            runStep(StepId.SignAPK, args.onEvent) {
                require(patchedApk.exists() && patchedApk.length() > 0L) {
                    "Patched APK was not generated"
                }
                keystoreManager.sign(patchedApk, File(args.output))
            }

            Log.i(tag, "Patching succeeded".logFmt())
            success = true
            Result.success()
        } catch (e: ProcessRuntime.RemoteFailureException) {
            Log.e(
                tag,
                "An exception occurred in the remote process while patching. ${e.originalStackTrace}".logFmt()
            )
            args.onEvent(
                ProgressEvent.Failed(
                    null,
                    e.toRemoteError()
                )
            ) // Fallback if exception doesn't occur within step
            Result.failure()
        } catch (e: Exception) {
            Log.e(tag, "An exception occurred while patching".logFmt(), e)
            args.onEvent(
                ProgressEvent.Failed(
                    null,
                    e.toRemoteError()
                )
            ) // Fallback if exception doesn't occur within step
            Result.failure()
        } finally {
            patchedApk.delete()
            if (!success) {
                File(args.output).delete()
            }
            // Only delete the input APK right after patching finishes when the user isn't rooted, since it would be needed for mounting
            // (it would be deleted right after installing with root)
            if (args.input is SelectedApp.Local && args.input.temporary && Shell.isAppGrantedRoot() == false) {
                args.input.file.delete()
            }
        }
    }

    companion object {
        private const val LOG_PREFIX = "[Worker]"
        private fun String.logFmt() = "$LOG_PREFIX $this"
    }
}