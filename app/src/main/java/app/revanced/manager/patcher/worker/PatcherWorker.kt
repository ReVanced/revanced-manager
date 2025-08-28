package app.revanced.manager.patcher.worker

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.worker.Worker
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.runtime.CoroutineRuntime
import app.revanced.manager.patcher.runtime.ProcessRuntime
import app.revanced.manager.plugin.downloader.GetScope
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.plugin.downloader.UserInteractionException
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.State
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.permissions.hasNotificationPermission
import app.revanced.manager.util.tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

typealias ProgressEventHandler = (name: String?, state: State?, message: String?) -> Unit

@OptIn(PluginHostApi::class)
class PatcherWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker<PatcherWorker.Args>(context, parameters), KoinComponent {
    private val workerRepository: WorkerRepository by inject()
    private val prefs: PreferencesManager by inject()
    private val keystoreManager: KeystoreManager by inject()
    private val downloaderPluginRepository: DownloaderPluginRepository by inject()
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
        val onDownloadProgress: suspend (Pair<Long, Long?>?) -> Unit,
        val onPatchCompleted: suspend () -> Unit,
        val handleStartActivityRequest: suspend (LoadedDownloaderPlugin, Intent) -> ActivityResult,
        val setInputFile: suspend (File) -> Unit,
        val onProgress: ProgressEventHandler
    ) {
        val packageName get() = input.packageName
    }

    val notificationChannelPatching = NotificationChannel(
        "revanced-patcher-patching", "Patching", NotificationManager.IMPORTANCE_LOW
    )

    val notificationChannelPatcherResult = NotificationChannel(
        "revanced-patcher-patcher-result", "Patching result", NotificationManager.IMPORTANCE_HIGH
    )

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val (patcherRunningNotification, _) = workerRepository.createNotification(
            applicationContext,
            notificationChannelPatching,
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE
            ),
            applicationContext.getString(R.string.patcher_notification_running_title),
            applicationContext.getString(R.string.patcher_notification_running_text),
            autoCancel = false
        )

        return ForegroundInfo(
                1,
                patcherRunningNotification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            Log.d(tag, "Android requested retrying but retrying is disabled.".logFmt())
            return Result.failure()
        }
        if (applicationContext.hasNotificationPermission()) {
            try {
                setForeground(getForegroundInfo())
            } catch (e: Exception) {
                Log.d(tag, "Failed to set foreground info:", e)
            }
        }

        val wakeLock: PowerManager.WakeLock =
            (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag::Patcher")
                .apply {
                    acquire(10 * 60 * 1000L)
                    Log.d(tag, "Acquired wakelock.")
                }

        val args = workerRepository.claimInput(this)

        return try {
            runPatcher(args)
        } finally {
            wakeLock.release()
        }
    }

    fun sendPatchingResultNotification(success: Boolean) {
        workerRepository.createNotification(
            applicationContext,
            notificationChannelPatcherResult,
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ),
            applicationContext.getString(
                if (success) R.string.patcher_notification_success_title
                else R.string.patcher_notification_failure_title
            ),
            applicationContext.getString(
                if (success) R.string.patcher_notification_success_text
                else R.string.patcher_notification_failure_text
            )
        ).also { (notification, notificationManager) ->
            if (applicationContext.hasNotificationPermission())
                notificationManager.notify(
                    System.currentTimeMillis()
                        .toInt(), // An unique ID, each patch process should show a notification
                    notification
                )
        }
    }


    enum class PatcherResult {
        SUCCESS, FAILURE, STOPPED
    }

    private suspend fun runPatcher(args: Args): Result {
        var patcherResult: PatcherResult = PatcherResult.FAILURE

        fun updateProgress(name: String? = null, state: State? = null, message: String? = null) =
            args.onProgress(name, state, message)

        val patchedApk = fs.tempDir.resolve("patched.apk")

        return try {
            if (args.input is SelectedApp.Installed) {
                installedAppRepository.get(args.packageName)?.let {
                    if (it.installType == InstallType.MOUNT) {
                        rootInstaller.unmount(args.packageName)
                    }
                }
            }

            suspend fun download(plugin: LoadedDownloaderPlugin, data: Parcelable) =
                downloadedAppRepository.download(
                    plugin,
                    data,
                    args.packageName,
                    args.input.version,
                    prefs.suggestedVersionSafeguard.get(),
                    !prefs.disablePatchVersionCompatCheck.get(),
                    onDownload = args.onDownloadProgress
                ).also {
                    args.setInputFile(it)
                    updateProgress(state = State.COMPLETED) // Download APK
                }

            val inputFile = when (val selectedApp = args.input) {
                is SelectedApp.Download -> {
                    val (plugin, data) = downloaderPluginRepository.unwrapParceledData(selectedApp.data)

                    download(plugin, data)
                }

                is SelectedApp.Search -> {
                    downloaderPluginRepository.loadedPluginsFlow.first()
                        .firstNotNullOfOrNull { plugin ->
                            try {
                                val getScope = object : GetScope {
                                    override val pluginPackageName = plugin.packageName
                                    override val hostPackageName = applicationContext.packageName
                                    override suspend fun requestStartActivity(intent: Intent): Intent? {
                                        val result = args.handleStartActivityRequest(plugin, intent)
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
                                    plugin.get(
                                        getScope,
                                        selectedApp.packageName,
                                        selectedApp.version
                                    )
                                }?.takeIf { (_, version) -> selectedApp.version == null || version == selectedApp.version }
                            } catch (e: UserInteractionException.Activity.NotCompleted) {
                                throw e
                            } catch (_: UserInteractionException) {
                                null
                            }?.let { (data, _) -> download(plugin, data) }
                        } ?: throw Exception("App is not available.")
                }

                is SelectedApp.Local -> selectedApp.file.also { args.setInputFile(it) }
                is SelectedApp.Installed -> File(pm.getPackageInfo(selectedApp.packageName)!!.applicationInfo!!.sourceDir)
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
                args.onPatchCompleted,
                args.onProgress
            )

            keystoreManager.sign(patchedApk, File(args.output))
            updateProgress(state = State.COMPLETED) // Signing

            Log.i(tag, "Patching succeeded".logFmt())
            patcherResult = PatcherResult.SUCCESS

            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            patcherResult = PatcherResult.STOPPED
            throw e
        } catch (e: ProcessRuntime.RemoteFailureException) {
            Log.e(
                tag,
                "An exception occurred in the remote process while patching. ${e.originalStackTrace}".logFmt()
            )
            updateProgress(state = State.FAILED, message = e.originalStackTrace)
            Result.failure()
        } catch (e: Exception) {
            Log.e(tag, "An exception occurred while patching".logFmt(), e)
            updateProgress(state = State.FAILED, message = e.stackTraceToString())
            Result.failure()
        } finally {
            when (patcherResult) {
                PatcherResult.SUCCESS -> sendPatchingResultNotification(success = true)
                PatcherResult.FAILURE -> sendPatchingResultNotification(success = false)
                else -> {}
            }

            patchedApk.delete()
            if (args.input is SelectedApp.Local && args.input.temporary) {
                args.input.file.delete()
            }
        }
    }

    companion object {
        private const val LOG_PREFIX = "[Worker]"
        private fun String.logFmt() = "$LOG_PREFIX $this"
    }
}
