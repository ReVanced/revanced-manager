package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.core.content.FileProvider
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.UserInteractionException
import app.revanced.manager.patcher.ProgressEvent
import app.revanced.manager.patcher.StepId
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.ui.model.InstallerModel
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.Step
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.model.navigation.Patcher
import app.revanced.manager.ui.model.withState
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.asCode
import app.revanced.manager.util.saveableVar
import app.revanced.manager.util.saver.snapshotStateListSaver
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.io.File
import java.nio.file.Files
import java.time.Duration
import android.content.pm.PackageInstaller as AndroidPackageInstaller

@OptIn(SavedStateHandleSaveableApi::class, DownloaderHostApi::class)
class PatcherViewModel(
    private val input: Patcher.ViewModelParams
) : ViewModel(), KoinComponent, InstallerModel {
    private val app: Application by inject()
    private val fs: Filesystem by inject()
    private val pm: PM by inject()
    private val workerRepository: WorkerRepository by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val patchBundleRepository: PatchBundleRepository by inject()
    private val rootInstaller: RootInstaller by inject()
    private val savedStateHandle: SavedStateHandle = get()
    private val ackpineInstaller: PackageInstaller = get()

    private var installedApp: InstalledApp? = null
    private val selectedApp = input.selectedApp
    val packageName = selectedApp.packageName
    val version = selectedApp.version

    var installedPackageName by savedStateHandle.saveable(
        key = "installedPackageName",
        // Force Kotlin to select the correct overload.
        stateSaver = autoSaver()
    ) {
        mutableStateOf<String?>(null)
    }
        private set
    var packageInstallerStatus: Int? by savedStateHandle.saveable(
        key = "packageInstallerStatus",
        stateSaver = autoSaver()
    ) {
        mutableStateOf(null)
    }
        private set

    var isInstalling by mutableStateOf(false)
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

    /**
     * This coroutine scope is used to await installations.
     * It should not be cancelled on system-initiated process death since that would cancel the installation process.
     */
    private val installerCoroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Holds the package name of the Apk we are trying to install.
     */
    private var installerPkgName: String by savedStateHandle.saveableVar { "" }
    private var installerSessionId: ParcelUuid? by savedStateHandle.saveableVar()

    private var inputFile: File? by savedStateHandle.saveableVar()
    private val outputFile = tempDir.resolve("output.apk")
    var preparedLogUri by mutableStateOf<Uri?>(null)
        private set

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
    val logPreviewText
        get() = logs.takeLast(30).joinToString("\n") { (level, msg) -> "[${level.name}]: $msg" }

    val steps by savedStateHandle.saveable(saver = snapshotStateListSaver()) {
        generateSteps(app, input.selectedApp, input.selectedPatches).toMutableStateList()
    }

    val progress by derivedStateOf {
        val steps = steps.filter { it.id != StepId.ExecutePatches }

        val current = steps.count { it.state == State.COMPLETED }
        val total = steps.size

        current.toFloat() / total.toFloat()
    }

    private val workManager = WorkManager.getInstance(app)

    private val patcherWorkerId by savedStateHandle.saveable<ParcelUuid> {
        ParcelUuid(
            workerRepository.launchExpedited<PatcherWorker, PatcherWorker.Args>(
                "patching", PatcherWorker.Args(
                    input.selectedApp,
                    outputFile.path,
                    input.selectedPatches,
                    input.options,
                    logger,
                    setInputFile = { withContext(Dispatchers.Main) { inputFile = it } },
                    handleStartActivityRequest = { downloader, intent ->
                        withContext(Dispatchers.Main) {
                            if (currentActivityRequest != null) throw Exception("Another request is already pending.")
                            try {
                                // Wait for the dialog interaction.
                                val accepted = with(CompletableDeferred<Boolean>()) {
                                    currentActivityRequest = this to downloader.name

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
                    onEvent = ::handleProgressEvent,
                )
            )
        )
    }

    val patcherSucceeded =
        workManager.getWorkInfoByIdLiveData(patcherWorkerId.uuid).map { workInfo: WorkInfo? ->
            when (workInfo?.state) {
                WorkInfo.State.SUCCEEDED -> true
                WorkInfo.State.FAILED -> false
                else -> null
            }
        }

    init {
        // TODO: detect system-initiated process death during the patching process.

        installerSessionId?.uuid?.let { id ->
            viewModelScope.launch {
                try {
                    isInstalling = true
                    uiSafe(app, R.string.install_app_fail, "Failed to install") {
                        // The process was killed during installation. Await the session again.
                        withContext(Dispatchers.IO) {
                            ackpineInstaller.getSession(id)
                        }?.let {
                            awaitInstallation(it)
                        }
                    }
                } finally {
                    isInstalling = false
                }
            }
        }

        viewModelScope.launch {
            installedApp = installedAppRepository.get(packageName)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
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

    private fun handleProgressEvent(event: ProgressEvent) = viewModelScope.launch {
        if (event is ProgressEvent.Failed && event.stepId == null && steps.any { it.state == State.FAILED }) {
            return@launch
        }

        val stepIndex = steps.indexOfFirst {
            event.stepId?.let { id -> id == it.id }
                ?: (it.state == State.RUNNING || it.state == State.WAITING)
        }

        if (stepIndex != -1) {
            val currentStep = steps[stepIndex]
            val updatedStep = when (event) {
                is ProgressEvent.Started -> currentStep.withState(State.RUNNING)

                is ProgressEvent.Progress -> currentStep.withState(
                    message = event.message ?: currentStep.message,
                    progress = event.current?.let { event.current to event.total } ?: currentStep.progress
                )

                is ProgressEvent.Log -> currentStep.withState(
                    message = appendLog(
                        currentStep.message,
                        formatLogLine(event.level, event.message)
                    )
                )

                is ProgressEvent.Completed -> currentStep.withState(State.COMPLETED, progress = null)
                    .let { step ->
                        if (step.id is StepId.ExecutePatch) step.copy(hide = false) else step
                    }

                is ProgressEvent.Failed -> currentStep.withState(
                    State.FAILED,
                    message = event.error.stackTrace,
                    progress = null
                ).let { step ->
                    if (step.id is StepId.ExecutePatch) step.copy(hide = false) else step
                }
            }

            steps[stepIndex] = updatedStep
        }
    }

    fun onBack() {
        installerCoroutineScope.cancel()
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

    fun logFileName() = "revanced_patcher_${packageName}_${version}_${System.currentTimeMillis()}.txt"

    fun prepareLogExport() = viewModelScope.launch {
        val uri = withContext(Dispatchers.IO) {
            tempDir.resolve(logFileName()).also {
                it.writeText(logs.joinToString("\n") {  (level, msg) -> formatLogLine(level, msg) }
            }.let {
                FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", it)
            }
        }

        preparedLogUri = uri
    }

    fun saveLogs(target: Uri?) = viewModelScope.launch {
        target?.let {
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(it)?.bufferedWriter().use { writer ->
                    writer?.write(logs.joinToString("\n") { (level, msg) -> "[${level.name}]: $msg" })
                }
            }
            app.toast(app.getString(R.string.save_logs_success))
        }
    }

    fun clearPreparedLogExport() {
        preparedLogUri = null
    }

    fun open() = installedPackageName?.let(pm::launch)

    private suspend fun startInstallation(file: File, packageName: String) {
        val session = withContext(Dispatchers.IO) {
            ackpineInstaller.createSession(Uri.fromFile(file)) {
                confirmation = Confirmation.IMMEDIATE
            }
        }
        withContext(Dispatchers.Main) {
            installerPkgName = packageName
        }
        awaitInstallation(session)
    }

    private suspend fun awaitInstallation(session: ProgressSession<InstallFailure>) = withContext(
        Dispatchers.Main
    ) {
        val result = installerCoroutineScope.async {
            try {
                installerSessionId = ParcelUuid(session.id)
                withContext(Dispatchers.IO) {
                    session.await()
                }
            } finally {
                installerSessionId = null
            }
        }.await()

        when (result) {
            is Session.State.Failed<InstallFailure> -> {
                result.failure.message?.let(logger::trace)
                packageInstallerStatus = result.failure.asCode()
            }

            Session.State.Succeeded -> {
                app.toast(app.getString(R.string.install_app_success))
                installedPackageName = installerPkgName
                val bundleInfo = patchBundleRepository.bundleInfoFlow.first()
                installedAppRepository.addOrUpdate(
                    installerPkgName,
                    packageName,
                    input.selectedApp.version
                        ?: withContext(Dispatchers.IO) { pm.getPackageInfo(outputFile)?.versionName!! },
                    InstallType.DEFAULT,
                    input.selectedPatches,
                    bundleInfo
                )
            }
        }
    }

    fun install(installType: InstallType) = viewModelScope.launch {
        isInstalling = true
        var needsRootUninstall = false
        try {
            uiSafe(app, R.string.install_app_fail, "Failed to install") {
                val currentPackageInfo =
                    withContext(Dispatchers.IO) { pm.getPackageInfo(outputFile) }
                        ?: throw Exception("Failed to load application info")


                when (installType) {
                    InstallType.DEFAULT -> {
                        // If the app is currently installed
                        val existingPackageInfo =
                            withContext(Dispatchers.IO) { pm.getPackageInfo(currentPackageInfo.packageName) }
                        if (existingPackageInfo != null) {
                            // Check if the app version is less than the installed version
                            if (
                                pm.getVersionCode(currentPackageInfo) < pm.getVersionCode(
                                    existingPackageInfo
                                )
                            ) {
                                // Exit if the selected app version is less than the installed version
                                packageInstallerStatus =
                                    AndroidPackageInstaller.STATUS_FAILURE_CONFLICT
                                return@launch
                            }
                        }

                        // Check if the app is mounted as root
                        // If it is, unmount it first, silently
                        if (rootInstaller.hasRootAccess() && rootInstaller.isAppMounted(packageName)) {
                            rootInstaller.unmount(packageName)
                        }

                        // Install regularly
                        startInstallation(outputFile, currentPackageInfo.packageName)
                    }

                    InstallType.MOUNT -> {
                        val label = with(pm) {
                            currentPackageInfo.label()
                        }

                        val inputVersion = input.selectedApp.version
                            ?: withContext(Dispatchers.IO) { inputFile?.let(pm::getPackageInfo)?.versionName }
                            ?: throw Exception("Failed to determine input APK version")

                        needsRootUninstall = true
                        // Install as root
                        rootInstaller.install(
                            outputFile,
                            inputFile,
                            packageName,
                            inputVersion,
                            label
                        )

                        val bundleInfo = patchBundleRepository.bundleInfoFlow.first()
                        installedAppRepository.addOrUpdate(
                            currentPackageInfo.packageName,
                            packageName,
                            inputVersion,
                            InstallType.MOUNT,
                            input.selectedPatches,
                            bundleInfo
                        )

                        rootInstaller.mount(packageName)

                        installedPackageName = packageName

                        app.toast(app.getString(R.string.install_app_success))
                        needsRootUninstall = false
                    }
                }
            }
        } finally {
            isInstalling = false
            if (needsRootUninstall) {
                try {
                    withContext(NonCancellable) {
                        rootInstaller.uninstall(packageName)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun install() {
        // InstallType.MOUNT is never used here since this overload is for the package installer status dialog.
        install(InstallType.DEFAULT)
    }

    override fun reinstall() {
        viewModelScope.launch {
            try {
                isInstalling = true
                uiSafe(app, R.string.reinstall_app_fail, "Failed to reinstall") {
                    val pkgName = withContext(Dispatchers.IO) {
                        pm.getPackageInfo(outputFile)?.packageName
                            ?: throw Exception("Failed to load application info")
                    }

                    when (val result = pm.uninstallPackage(pkgName)) {
                        is Session.State.Failed<UninstallFailure> -> {
                            result.failure.message?.let(logger::trace)
                            packageInstallerStatus = result.failure.asCode()
                            return@launch
                        }

                        Session.State.Succeeded -> {}
                    }
                    startInstallation(outputFile, pkgName)
                }
            } finally {
                isInstalling = false
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

        fun formatLogLine(level: LogLevel, message: String) = when (level) {
            LogLevel.INFO -> message
            LogLevel.WARN -> "Warning: $message"
            LogLevel.ERROR -> "Error: $message"
            LogLevel.TRACE -> "Debug: $message"
        }

        fun appendLog(current: String?, line: String): String =
            current?.takeIf { it.isNotBlank() }
                ?.let { "$it\n$line" }
                ?: line

        fun generateSteps(
            context: Context,
            selectedApp: SelectedApp,
            selectedPatches: PatchSelection
        ): List<Step> = buildList {
            if (selectedApp is SelectedApp.Download || selectedApp is SelectedApp.Search)
                add(
                    Step(
                        StepId.DownloadAPK,
                        context.getString(R.string.download_apk),
                        StepCategory.PREPARING
                    )
                )

            add(
                Step(
                    StepId.LoadPatches,
                    context.getString(R.string.patcher_step_load_patches),
                    StepCategory.PREPARING
                )
            )
            add(
                Step(
                    StepId.ReadAPK,
                    context.getString(R.string.patcher_step_unpack),
                    StepCategory.PREPARING
                )
            )
            add(
                Step(
                    StepId.ExecutePatches,
                    context.getString(R.string.execute_patches),
                    StepCategory.PATCHING
                )
            )

            selectedPatches.values.asSequence().flatten().forEachIndexed { index, name ->
                add(
                    Step(
                        StepId.ExecutePatch(index),
                        name,
                        StepCategory.PATCHING,
                        hide = true
                    )
                )
            }

            add(
                Step(
                    StepId.WriteAPK,
                    context.getString(R.string.patcher_step_write_patched),
                    StepCategory.SAVING
                )
            )
            add(
                Step(
                    StepId.SignAPK,
                    context.getString(R.string.patcher_step_sign_apk),
                    StepCategory.SAVING
                )
            )
        }
    }
}