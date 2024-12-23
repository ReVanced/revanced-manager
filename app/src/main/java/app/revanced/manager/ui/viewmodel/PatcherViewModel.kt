package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
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
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.plugin.downloader.UserInteractionException
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.ui.model.InstallerModel
import app.revanced.manager.ui.model.ProgressKey
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.Step
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.model.StepProgressProvider
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.util.PM
import app.revanced.manager.util.saveableVar
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Files
import java.time.Duration

@OptIn(SavedStateHandleSaveableApi::class, PluginHostApi::class)
class PatcherViewModel(
    private val input: Patcher.ViewModelParams
) : ViewModel(), KoinComponent, StepProgressProvider, InstallerModel {
    private val app: Application by inject()
    private val fs: Filesystem by inject()
    private val pm: PM by inject()
    private val workerRepository: WorkerRepository by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val rootInstaller: RootInstaller by inject()
    private val savedStateHandle: SavedStateHandle = get()

    private var installedApp: InstalledApp? = null
    val packageName = input.selectedApp.packageName

    var installedPackageName by savedStateHandle.saveable(
        key = "installedPackageName",
        // Force Kotlin to select the correct overload.
        stateSaver = autoSaver()
    ) {
        mutableStateOf<String?>(null)
    }
        private set
    private var ongoingPmSession: Boolean by savedStateHandle.saveableVar { false }
    var packageInstallerStatus: Int? by savedStateHandle.saveable(
        key = "packageInstallerStatus",
        stateSaver = autoSaver()
    ) {
        mutableStateOf(null)
    }
        private set

    var isInstalling by mutableStateOf(ongoingPmSession)
        private set

    private var currentActivityRequest: Pair<CompletableDeferred<Boolean>, String>? by mutableStateOf(
        null
    )
    val activityPromptDialog by derivedStateOf { currentActivityRequest?.second }

    private var launchedActivity: CompletableDeferred<ActivityResult>? = null
    private val launchActivityChannel = Channel<Intent>()
    val launchActivityFlow = launchActivityChannel.receiveAsFlow()

    private val tempDir = savedStateHandle.saveable(key = "tempDir") {
        fs.uiTempDir.resolve("installer").also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    private var inputFile: File? by savedStateHandle.saveableVar()
    private val outputFile = tempDir.resolve("output.apk")

    private val logs by savedStateHandle.saveable<MutableList<Pair<LogLevel, String>>> { mutableListOf() }
    private val logger = object : Logger() {
        override fun log(level: LogLevel, message: String) {
            level.androidLog(message)
            if (level == LogLevel.TRACE) return

            viewModelScope.launch {
                logs.add(level to message)
            }
        }
    }

    private val patchCount = input.selectedPatches.values.sumOf { it.size }
    private var completedPatchCount by savedStateHandle.saveable {
        // SavedStateHandle.saveable only supports the boxed version.
        @Suppress("AutoboxingStateCreation") mutableStateOf(
            0
        )
    }
    val patchesProgress get() = completedPatchCount to patchCount
    override var downloadProgress by savedStateHandle.saveable(
        key = "downloadProgress",
        stateSaver = autoSaver()
    ) {
        mutableStateOf<Pair<Long, Long?>?>(null)
    }
        private set
    val steps by savedStateHandle.saveable(saver = snapshotStateListSaver()) {
        generateSteps(
            app,
            input.selectedApp
        ).toMutableStateList()
    }
    private var currentStepIndex = 0

    val progress by derivedStateOf {
        val current = steps.count {
            it.state == State.COMPLETED && it.category != StepCategory.PATCHING
        } + completedPatchCount

        val total = steps.size - 1 + patchCount

        current.toFloat() / total.toFloat()
    }

    private val workManager = WorkManager.getInstance(app)

    private val patcherWorkerId by savedStateHandle.saveable<ParcelUuid> {
        ParcelUuid(workerRepository.launchExpedited<PatcherWorker, PatcherWorker.Args>(
            "patching", PatcherWorker.Args(
                input.selectedApp,
                outputFile.path,
                input.selectedPatches,
                input.options,
                logger,
                onDownloadProgress = {
                    withContext(Dispatchers.Main) {
                        downloadProgress = it
                    }
                },
                onPatchCompleted = { withContext(Dispatchers.Main) { completedPatchCount += 1 } },
                setInputFile = { withContext(Dispatchers.Main) { inputFile = it } },
                handleStartActivityRequest = { plugin, intent ->
                    withContext(Dispatchers.Main) {
                        if (currentActivityRequest != null) throw Exception("Another request is already pending.")
                        try {
                            // Wait for the dialog interaction.
                            val accepted = with(CompletableDeferred<Boolean>()) {
                                currentActivityRequest = this to plugin.name

                                await()
                            }
                            if (!accepted) throw UserInteractionException.RequestDenied()

                            // Launch the activity and wait for the result.
                            try {
                                with(CompletableDeferred<ActivityResult>()) {
                                    launchedActivity = this
                                    launchActivityChannel.send(intent)
                                    await()
                                }
                            } finally {
                                launchedActivity = null
                            }
                        } finally {
                            currentActivityRequest = null
                        }
                    }
                },
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
        ))
    }

    val patcherSucceeded =
        workManager.getWorkInfoByIdLiveData(patcherWorkerId.uuid).map { workInfo: WorkInfo? ->
            when (workInfo?.state) {
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
                        app.toast(app.getString(R.string.install_app_success))
                        installedPackageName =
                            intent.getStringExtra(InstallService.EXTRA_PACKAGE_NAME)
                        viewModelScope.launch {
                            installedAppRepository.addOrUpdate(
                                installedPackageName!!,
                                packageName,
                                input.selectedApp.version
                                    ?: pm.getPackageInfo(outputFile)?.versionName!!,
                                InstallType.DEFAULT,
                                input.selectedPatches
                            )
                        }
                    } else packageInstallerStatus = pmStatus

                    isInstalling = false
                }

                UninstallService.APP_UNINSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(
                        UninstallService.EXTRA_UNINSTALL_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )

                    intent.getStringExtra(UninstallService.EXTRA_UNINSTALL_STATUS_MESSAGE)
                        ?.let(logger::trace)

                    if (pmStatus != PackageInstaller.STATUS_SUCCESS)
                        packageInstallerStatus = pmStatus
                }
            }
        }
    }

    init {
        // TODO: detect system-initiated process death during the patching process.
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
        workManager.cancelWorkById(patcherWorkerId.uuid)

        if (input.selectedApp is SelectedApp.Installed && installedApp?.installType == InstallType.MOUNT) {
            GlobalScope.launch(Dispatchers.Main) {
                uiSafe(app, R.string.failed_to_mount, "Failed to mount") {
                    withTimeout(Duration.ofMinutes(1L)) {
                        rootInstaller.mount(packageName)
                    }
                }
            }
        }
    }

    fun onBack() {
        // tempDir cannot be deleted inside onCleared because it gets called on system-initiated process death.
        tempDir.deleteRecursively()
    }

    fun isDeviceRooted() = rootInstaller.isDeviceRooted()

    fun rejectInteraction() {
        currentActivityRequest?.first?.complete(false)
    }

    fun allowInteraction() {
        currentActivityRequest?.first?.complete(true)
    }

    fun handleActivityResult(result: ActivityResult) {
        launchedActivity?.complete(result)
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
                    packageInstallerStatus = PackageInstaller.STATUS_FAILURE_CONFLICT
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

                InstallType.MOUNT -> {
                    try {
                        val packageInfo = pm.getPackageInfo(outputFile)
                            ?: throw Exception("Failed to load application info")
                        val label = with(pm) {
                            packageInfo.label()
                        }

                        // Check for base APK, first check if the app is already installed
                        if (existingPackageInfo == null) {
                            // If the app is not installed, check if the output file is a base apk
                            if (currentPackageInfo.splitNames.isNotEmpty()) {
                                // Exit if there is no base APK package
                                packageInstallerStatus = PackageInstaller.STATUS_FAILURE_INVALID
                                return@launch
                            }
                        }

                        val inputVersion = input.selectedApp.version
                            ?: inputFile?.let(pm::getPackageInfo)?.versionName
                            ?: throw Exception("Failed to determine input APK version")

                        // Install as root
                        rootInstaller.install(
                            outputFile,
                            inputFile,
                            packageName,
                            inputVersion,
                            label
                        )

                        installedAppRepository.addOrUpdate(
                            packageInfo.packageName,
                            packageName,
                            inputVersion,
                            InstallType.MOUNT,
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to install", e)
            app.toast(app.getString(R.string.install_app_fail, e.simpleMessage()))
        } finally {
            if (!pmInstallStarted) isInstalling = false
        }
    }

    override fun install() {
        // InstallType.MOUNT is never used here since this overload is for the package installer status dialog.
        install(InstallType.DEFAULT)
    }

    override fun reinstall() {
        viewModelScope.launch {
            uiSafe(app, R.string.reinstall_app_fail, "Failed to reinstall") {
                pm.getPackageInfo(outputFile)?.packageName?.let { pm.uninstallPackage(it) }
                    ?: throw Exception("Failed to load application info")

                pm.installApp(listOf(outputFile))
                isInstalling = true
            }
        }
    }

    fun dismissPackageInstallerDialog() {
        packageInstallerStatus = null
    }

    private companion object {
        const val TAG = "ReVanced Patcher"

        fun LogLevel.androidLog(msg: String) = when (this) {
            LogLevel.TRACE -> Log.v(TAG, msg)
            LogLevel.INFO -> Log.i(TAG, msg)
            LogLevel.WARN -> Log.w(TAG, msg)
            LogLevel.ERROR -> Log.e(TAG, msg)
        }

        fun generateSteps(context: Context, selectedApp: SelectedApp): List<Step> {
            val needsDownload =
                selectedApp is SelectedApp.Download || selectedApp is SelectedApp.Search

            return listOfNotNull(
                Step(
                    context.getString(R.string.download_apk),
                    StepCategory.PREPARING,
                    state = State.RUNNING,
                    progressKey = ProgressKey.DOWNLOAD,
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