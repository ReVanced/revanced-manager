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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import app.revanced.manager.patcher.worker.PatcherProgressManager
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.patcher.worker.Step
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.ui.destination.Destination
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.PM
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.logging.Level
import java.util.logging.LogRecord

@Stable
class InstallerViewModel(
    private val input: Destination.Installer
) : ViewModel(), KoinComponent {
    private val app: Application by inject()
    private val fs: Filesystem by inject()
    private val pm: PM by inject()
    private val workerRepository: WorkerRepository by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    private val rootInstaller: RootInstaller by inject()

    val packageName: String = input.selectedApp.packageName
    private val tempDir = fs.tempDir.resolve("installer").also {
        it.deleteRecursively()
        it.mkdirs()
    }

    private val outputFile = tempDir.resolve("output.apk")
    private var inputFile: File? = null

    private var installedApp: InstalledApp? = null
    var isInstalling by mutableStateOf(false)
        private set
    var installedPackageName by mutableStateOf<String?>(null)
        private set
    val appButtonText by derivedStateOf { if (installedPackageName == null) R.string.install_app else R.string.open_app }

    private val workManager = WorkManager.getInstance(app)

    private val _progress: MutableStateFlow<ImmutableList<Step>>
    private val patcherWorkerId: UUID
    private val logger = ManagerLogger()

    init {
        // TODO: navigate away when system-initiated process death is detected because it is not possible to recover from it.

        viewModelScope.launch {
            installedApp = installedAppRepository.get(packageName)
        }

        val (selectedApp, patches, options) = input

        _progress = MutableStateFlow(
            PatcherProgressManager.generateSteps(
                app,
                patches.flatMap { (_, selected) -> selected },
                selectedApp
            ).toImmutableList()
        )

        patcherWorkerId =
            workerRepository.launchExpedited<PatcherWorker, PatcherWorker.Args>(
                "patching", PatcherWorker.Args(
                    selectedApp,
                    outputFile.path,
                    patches,
                    options,
                    _progress,
                    logger,
                    setInputFile = { inputFile = it }
                )
            )
    }

    val progress = _progress.asStateFlow()

    val patcherState =
        workManager.getWorkInfoByIdLiveData(patcherWorkerId).map { workInfo: WorkInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> true
                WorkInfo.State.FAILED -> false
                else -> null
            }
        }

    private val installBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallService.APP_INSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(InstallService.EXTRA_INSTALL_STATUS, -999)
                    val extra = intent.getStringExtra(InstallService.EXTRA_INSTALL_STATUS_MESSAGE)!!

                    if (pmStatus == PackageInstaller.STATUS_SUCCESS) {
                        app.toast(app.getString(R.string.install_app_success))
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
                    } else {
                        app.toast(app.getString(R.string.install_app_fail, extra))
                    }
                }

                UninstallService.APP_UNINSTALL_ACTION -> {
                }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(app, installBroadcastReceiver, IntentFilter().apply {
            addAction(InstallService.APP_INSTALL_ACTION)
            addAction(UninstallService.APP_UNINSTALL_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun exportLogs(context: Context) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logger.export())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(installBroadcastReceiver)
        workManager.cancelWorkById(patcherWorkerId)

        when (val selectedApp = input.selectedApp) {
            is SelectedApp.Local -> {
                if (selectedApp.shouldDelete) selectedApp.file.delete()
            }

            is SelectedApp.Installed -> {
                try {
                    installedApp?.let {
                        if (it.installType == InstallType.ROOT) {
                            rootInstaller.mount(packageName)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to mount", e)
                    app.toast(app.getString(R.string.failed_to_mount, e.simpleMessage()))
                }
            }

            else -> {}
        }

        tempDir.deleteRecursively()
    }

    fun export(uri: Uri?) = viewModelScope.launch {
        uri?.let {
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(it)
                    .use { stream -> Files.copy(outputFile.toPath(), stream) }
            }
            app.toast(app.getString(R.string.export_app_success))
        }
    }

    fun install(installType: InstallType) = viewModelScope.launch {
        isInstalling = true
        try {
            when (installType) {
                InstallType.DEFAULT -> {
                    pm.installApp(listOf(outputFile))
                }

                InstallType.ROOT -> {
                    installAsRoot()
                }
            }

        } finally {
            isInstalling = false
        }
    }

    fun open() = installedPackageName?.let { pm.launch(it) }

    private suspend fun installAsRoot() {
        try {
            val label = with(pm) {
                getPackageInfo(outputFile)?.label()
                    ?: throw Exception("Failed to load application info")
            }

            rootInstaller.install(
                outputFile,
                inputFile,
                packageName,
                input.selectedApp.version,
                label
            )

            rootInstaller.mount(packageName)

            installedApp?.let { installedAppRepository.delete(it) }

            installedAppRepository.addOrUpdate(
                packageName,
                packageName,
                input.selectedApp.version,
                InstallType.ROOT,
                input.selectedPatches
            )

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

// TODO: move this to a better place
class ManagerLogger : java.util.logging.Handler() {
    private val logs = mutableListOf<Pair<LogLevel, String>>()
    private fun log(level: LogLevel, msg: String) {
        level.androidLog(msg)
        if (level == LogLevel.TRACE) return
        logs.add(level to msg)
    }

    fun export() =
        logs.asSequence().map { (level, msg) -> "[${level.name}]: $msg" }.joinToString("\n")

    fun trace(msg: String) = log(LogLevel.TRACE, msg)
    fun info(msg: String) = log(LogLevel.INFO, msg)
    fun warn(msg: String) = log(LogLevel.WARN, msg)
    fun error(msg: String) = log(LogLevel.ERROR, msg)
    override fun publish(record: LogRecord) {
        val msg = record.message
        val fn = when (record.level) {
            Level.INFO -> ::info
            Level.SEVERE -> ::error
            Level.WARNING -> ::warn
            else -> ::trace
        }

        fn(msg)
    }

    override fun flush() = Unit

    override fun close() = Unit
}

enum class LogLevel {
    TRACE {
        override fun androidLog(msg: String) = Log.v(androidTag, msg)
    },
    INFO {
        override fun androidLog(msg: String) = Log.i(androidTag, msg)
    },
    WARN {
        override fun androidLog(msg: String) = Log.w(androidTag, msg)
    },
    ERROR {
        override fun androidLog(msg: String) = Log.e(androidTag, msg)
    };

    abstract fun androidLog(msg: String): Int

    private companion object {
        const val androidTag = "ReVanced Patcher"
    }
}