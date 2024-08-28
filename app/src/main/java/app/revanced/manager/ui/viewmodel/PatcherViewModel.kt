package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.ui.component.InstallerStatusDialogModel
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.Step
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.util.PM
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.UUID

@Stable
class PatcherViewModel(
    private val input: Destination.Patcher
) : ViewModel(), KoinComponent {
    private val app: Application by inject()
    private val fs: Filesystem by inject()
    private val pm: PM by inject()
    private val workerRepository: WorkerRepository by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val rootInstaller: RootInstaller by inject()

    val installerStatusDialogModel : InstallerStatusDialogModel = object : InstallerStatusDialogModel {
        override var packageInstallerStatus: Int? by mutableStateOf(null)

        override fun reinstall() {
            this@PatcherViewModel.reinstall()
        }

        override fun install() {
            // Since this is a package installer status dialog,
            // InstallType.ROOT is never used here.
            install(InstallType.DEFAULT)
        }
    }

    private var installedApp: InstalledApp? = null
    val packageName: String = input.selectedApp.packageName
    var installedPackageName by mutableStateOf<String?>(null)
        private set
    var isInstalling by mutableStateOf(false)
        private set

    private val tempDir = fs.tempDir.resolve("installer").also {
        it.deleteRecursively()
        it.mkdirs()
    }
    private var inputFile: File? = null
    private val outputFile = tempDir.resolve("output.apk")

    private val logs = mutableListOf<Pair<LogLevel, String>>()
    private val logger = object : Logger() {
        override fun log(level: LogLevel, message: String) {
            level.androidLog(message)
            if (level == LogLevel.TRACE) return

            viewModelScope.launch {
                logs.add(level to message)
            }
        }
    }

    val patchesProgress = MutableStateFlow(Pair(0, input.selectedPatches.values.sumOf { it.size }))
    private val downloadProgress = MutableStateFlow<Pair<Float, Float>?>(null)
    val steps = generateSteps(
        app,
        input.selectedApp,
        downloadProgress
    ).toMutableStateList()
    private var currentStepIndex = 0

    private val workManager = WorkManager.getInstance(app)

    private val patcherWorkerId: UUID =
        workerRepository.launchExpedited<PatcherWorker, PatcherWorker.Args>(
            "patching", PatcherWorker.Args(
                input.selectedApp,
                outputFile.path,
                input.selectedPatches,
                input.options,
                logger,
                downloadProgress,
                patchesProgress,
                setInputFile = { inputFile = it },
                onProgress = { name, state, message ->
                    viewModelScope.launch {
                        steps[currentStepIndex] = steps[currentStepIndex].run {
                            copy(
                                name = name ?: this.name,
                                state = state ?: this.state,
                                message = message ?: this.message
                            )
                        }

                        if (state == State.COMPLETED && currentStepIndex != steps.lastIndex) {
                            currentStepIndex++

                            steps[currentStepIndex] =
                                steps[currentStepIndex].copy(state = State.RUNNING)
                        }
                    }
                }
            )
        )

    val patcherSucceeded =
        workManager.getWorkInfoByIdLiveData(patcherWorkerId).map { workInfo: WorkInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> true
                WorkInfo.State.FAILED -> false
                else -> null
            }
        }

    private val installerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallService.APP_INSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(
                        InstallService.EXTRA_INSTALL_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )

                    intent.getStringExtra(UninstallService.EXTRA_UNINSTALL_STATUS_MESSAGE)
                        ?.let(logger::trace)

                    if (pmStatus == PackageInstaller.STATUS_SUCCESS) {
                        installedPackageName =
                            intent.getStringExtra(InstallService.EXTRA_PACKAGE_NAME)
                        viewModelScope.launch {
                            installedAppRepository.addOrUpdate(
                                installedPackageName!!,
                                packageName,
                                input.selectedApp.version,
                                InstallType.DEFAULT,
                                input.selectedPatches
                            )
                        }
                    }

                    installerStatusDialogModel.packageInstallerStatus = pmStatus

                    isInstalling = false
                }

                UninstallService.APP_UNINSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(
                        UninstallService.EXTRA_UNINSTALL_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )

                    intent.getStringExtra(UninstallService.EXTRA_UNINSTALL_STATUS_MESSAGE)
                        ?.let(logger::trace)

                    if (pmStatus != PackageInstaller.STATUS_SUCCESS) {
                        installerStatusDialogModel.packageInstallerStatus = pmStatus
                    }
                }
            }
        }
    }

    init { // TODO: navigate away when system-initiated process death is detected because it is not possible to recover from it.
        ContextCompat.registerReceiver(
            app,
            installerBroadcastReceiver,
            IntentFilter().apply {
                addAction(InstallService.APP_INSTALL_ACTION)
                addAction(UninstallService.APP_UNINSTALL_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        viewModelScope.launch {
            installedApp = installedAppRepository.get(packageName)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(installerBroadcastReceiver)
        workManager.cancelWorkById(patcherWorkerId)

        if (input.selectedApp is SelectedApp.Installed && installedApp?.installType == InstallType.ROOT) {
            GlobalScope.launch(Dispatchers.Main) {
                uiSafe(app, R.string.failed_to_mount, "Failed to mount") {
                    withTimeout(Duration.ofMinutes(1L)) {
                        rootInstaller.mount(packageName)
                    }
                }
            }
        }

        tempDir.deleteRecursively()
    }

    fun export(uri: Uri?) = viewModelScope.launch {
        uri?.let {
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(it)
                    .use { stream -> Files.copy(outputFile.toPath(), stream) }
            }
            app.toast(app.getString(R.string.save_apk_success))
        }
    }

    fun exportLogs(context: Context) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                logs.asSequence().map { (level, msg) -> "[${level.name}]: $msg" }.joinToString("\n")
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    fun open() = installedPackageName?.let(pm::launch)

    fun install(installType: InstallType) = viewModelScope.launch {
        var pmInstallStarted = false
        try {
            isInstalling = true

            val currentPackageInfo = pm.getPackageInfo(outputFile)
                ?: throw Exception("Failed to load application info")

            // If the app is currently installed
            val existingPackageInfo = pm.getPackageInfo(currentPackageInfo.packageName)
            if (existingPackageInfo != null) {
                // Check if the app version is less than the installed version
                if (pm.getVersionCode(currentPackageInfo) < pm.getVersionCode(existingPackageInfo)) {
                    // Exit if the selected app version is less than the installed version
                    installerStatusDialogModel.packageInstallerStatus = PackageInstaller.STATUS_FAILURE_CONFLICT
                    return@launch
                }
            }

            when (installType) {
                InstallType.DEFAULT -> {
                    // Check if the app is mounted as root
                    // If it is, unmount it first, silently
                    if (rootInstaller.hasRootAccess() && rootInstaller.isAppMounted(packageName)) {
                        rootInstaller.unmount(packageName)
                    }

                    // Install regularly
                    pm.installApp(listOf(outputFile))
                    pmInstallStarted = true
                }

                InstallType.ROOT -> {
                    try {
                        // Check for base APK, first check if the app is already installed
                        if (existingPackageInfo == null) {
                            // If the app is not installed, check if the output file is a base apk
                            if (currentPackageInfo.splitNames != null) {
                                // Exit if there is no base APK package
                                installerStatusDialogModel.packageInstallerStatus =
                                    PackageInstaller.STATUS_FAILURE_INVALID
                                return@launch
                            }
                        }

                        // Get label
                        val label = with(pm) {
                            currentPackageInfo.label()
                        }

                        // Install as root
                        rootInstaller.install(
                            outputFile,
                            inputFile,
                            packageName,
                            input.selectedApp.version,
                            label
                        )

                        installedAppRepository.addOrUpdate(
                            packageName,
                            packageName,
                            input.selectedApp.version,
                            InstallType.ROOT,
                            input.selectedPatches
                        )

                        rootInstaller.mount(packageName)

                        installedPackageName = packageName

                        app.toast(app.getString(R.string.install_app_success))
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to install as root", e)
                        app.toast(app.getString(R.string.install_app_fail, e.simpleMessage()))
                        try {
                            rootInstaller.uninstall(packageName)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch(e: Exception) {
            Log.e(tag, "Failed to install", e)
            app.toast(app.getString(R.string.install_app_fail, e.simpleMessage()))
        } finally {
            if (!pmInstallStarted)
                isInstalling = false
        }
    }

    fun reinstall() = viewModelScope.launch {
        uiSafe(app, R.string.reinstall_app_fail, "Failed to reinstall") {
            pm.getPackageInfo(outputFile)?.packageName?.let { pm.uninstallPackage(it) }
                ?: throw Exception("Failed to load application info")

            pm.installApp(listOf(outputFile))
            isInstalling = true
        }
    }

    companion object {
        private const val TAG = "ReVanced Patcher"

        fun LogLevel.androidLog(msg: String) = when (this) {
            LogLevel.TRACE -> Log.v(TAG, msg)
            LogLevel.INFO -> Log.i(TAG, msg)
            LogLevel.WARN -> Log.w(TAG, msg)
            LogLevel.ERROR -> Log.e(TAG, msg)
        }

        fun generateSteps(
            context: Context,
            selectedApp: SelectedApp,
            downloadProgress: StateFlow<Pair<Float, Float>?>? = null
        ): List<Step> {
            val needsDownload = selectedApp is SelectedApp.Download

            return listOfNotNull(
                Step(
                    context.getString(R.string.download_apk),
                    StepCategory.PREPARING,
                    state = State.RUNNING,
                    downloadProgress = downloadProgress,
                ).takeIf { needsDownload },
                Step(
                    context.getString(R.string.patcher_step_load_patches),
                    StepCategory.PREPARING,
                    state = if (needsDownload) State.WAITING else State.RUNNING,
                ),
                Step(
                    context.getString(R.string.patcher_step_unpack),
                    StepCategory.PREPARING
                ),

                Step(
                    context.getString(R.string.execute_patches),
                    StepCategory.PATCHING
                ),

                Step(context.getString(R.string.patcher_step_write_patched), StepCategory.SAVING),
                Step(context.getString(R.string.patcher_step_sign_apk), StepCategory.SAVING)
            )
        }
    }
}