package app.revanced.manager.compose.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.*
import app.revanced.manager.compose.patcher.worker.PatcherWorker
import app.revanced.manager.compose.patcher.worker.PatcherProgressManager
import app.revanced.manager.compose.patcher.worker.StepGroup
import app.revanced.manager.compose.service.InstallService
import app.revanced.manager.compose.service.UninstallService
import app.revanced.manager.compose.util.PM
import app.revanced.manager.compose.util.PackageInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class InstallerScreenViewModel(
    input: PackageInfo,
    selectedPatches: List<String>,
    private val app: Application
) : ViewModel() {
    var stepGroups by mutableStateOf<List<StepGroup>>(PatcherProgressManager.generateGroupsList(app, selectedPatches))
        private set

    private val workManager = WorkManager.getInstance(app)

    // TODO: handle app installation as a step.
    var installStatus by mutableStateOf<Boolean?>(null)
    var pmStatus by mutableStateOf(-999)
    var extra by mutableStateOf("")

    private val outputFile = File(app.cacheDir, "output.apk")

    private val patcherWorker =
        OneTimeWorkRequest.Builder(PatcherWorker::class.java) // create Worker
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).setInputData(
                workDataOf(
                    PatcherWorker.ARGS_KEY to
                            Json.Default.encodeToString(
                                PatcherWorker.Args(
                                    input.apk.path,
                                    outputFile.path,
                                    selectedPatches,
                                    input.packageName,
                                    input.packageName,
                                )
                            )
                )
            ).build()

    private val liveData = workManager.getWorkInfoByIdLiveData(patcherWorker.id) // get LiveData

    private val observer = Observer { workInfo: WorkInfo -> // observer for observing patch status
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> workInfo.progress
            WorkInfo.State.FAILED, WorkInfo.State.SUCCEEDED -> workInfo.outputData
            else -> null
        }?.let { PatcherProgressManager.groupsFromWorkData(it) }?.let { stepGroups = it }
    }

    private val installBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallService.APP_INSTALL_ACTION -> {
                    pmStatus = intent.getIntExtra(InstallService.EXTRA_INSTALL_STATUS, -999)
                    extra = intent.getStringExtra(InstallService.EXTRA_INSTALL_STATUS_MESSAGE)!!
                    postInstallStatus()
                }

                UninstallService.APP_UNINSTALL_ACTION -> {
                }
            }
        }
    }

    init {
        workManager.enqueueUniqueWork("patching", ExistingWorkPolicy.KEEP, patcherWorker)
        liveData.observeForever(observer)
        app.registerReceiver(installBroadcastReceiver, IntentFilter().apply {
            addAction(InstallService.APP_INSTALL_ACTION)
            addAction(UninstallService.APP_UNINSTALL_ACTION)
        })
    }

    fun installApk(apk: List<File>) {
        PM.installApp(apk, app)
    }

    fun postInstallStatus() {
        installStatus = pmStatus == PackageInstaller.STATUS_SUCCESS
    }

    override fun onCleared() {
        super.onCleared()
        liveData.removeObserver(observer)
        app.unregisterReceiver(installBroadcastReceiver)
        workManager.cancelWorkById(patcherWorker.id)
        // logs.clear()
    }
}