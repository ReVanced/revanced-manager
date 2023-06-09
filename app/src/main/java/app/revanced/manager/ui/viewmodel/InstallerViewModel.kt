package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.work.*
import app.revanced.manager.R
import app.revanced.manager.patcher.SignerService
import app.revanced.manager.patcher.worker.PatcherProgressManager
import app.revanced.manager.patcher.worker.PatcherWorker
import app.revanced.manager.patcher.worker.StepGroup
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import app.revanced.manager.util.AppInfo
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.toast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Files

@Stable
class InstallerViewModel(
    input: AppInfo,
    selectedPatches: PatchesSelection
) : ViewModel(), KoinComponent {
    private val signerService: SignerService by inject()
    private val app: Application by inject()
    private val pm: PM by inject()

    val packageName: String = input.packageName
    private val outputFile = File(app.cacheDir, "output.apk")
    private val signedFile = File(app.cacheDir, "signed.apk").also { if (it.exists()) it.delete() }
    private var hasSigned = false

    var isInstalling by mutableStateOf(false)
        private set
    var installedPackageName by mutableStateOf<String?>(null)
        private set
    val appButtonText by derivedStateOf { if (installedPackageName == null) R.string.install_app else R.string.open_app }

    private val workManager = WorkManager.getInstance(app)
    private val patcherWorker =
        OneTimeWorkRequest.Builder(PatcherWorker::class.java) // create Worker
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).setInputData(
                workDataOf(
                    PatcherWorker.ARGS_KEY to
                            Json.Default.encodeToString(
                                PatcherWorker.Args(
                                    input.path!!.absolutePath,
                                    outputFile.path,
                                    selectedPatches,
                                    input.packageName,
                                    input.packageInfo!!.versionName,
                                )
                            )
                )
            ).build()

    val initialState = PatcherState(
        status = null,
        stepGroups = PatcherProgressManager.generateGroupsList(
            app,
            selectedPatches.flatMap { (_, selected) -> selected }
        )
    )
    val patcherState =
        workManager.getWorkInfoByIdLiveData(patcherWorker.id).map { workInfo: WorkInfo ->
            var status: Boolean? = null
            val stepGroups = when (workInfo.state) {
                WorkInfo.State.RUNNING -> workInfo.progress
                WorkInfo.State.FAILED, WorkInfo.State.SUCCEEDED -> workInfo.outputData.also {
                    status = workInfo.state == WorkInfo.State.SUCCEEDED
                }

                else -> null
            }?.let { PatcherProgressManager.groupsFromWorkData(it) }

            PatcherState(status, stepGroups ?: initialState.stepGroups)
        }

    private val installBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallService.APP_INSTALL_ACTION -> {
                    val pmStatus = intent.getIntExtra(InstallService.EXTRA_INSTALL_STATUS, -999)
                    val extra = intent.getStringExtra(InstallService.EXTRA_INSTALL_STATUS_MESSAGE)!!

                    if (pmStatus == PackageInstaller.STATUS_SUCCESS) {
                        app.toast(app.getString(R.string.install_app_success))
                        installedPackageName = intent.getStringExtra(InstallService.EXTRA_PACKAGE_NAME)
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
        workManager.enqueueUniqueWork("patching", ExistingWorkPolicy.KEEP, patcherWorker)
        app.registerReceiver(installBroadcastReceiver, IntentFilter().apply {
            addAction(InstallService.APP_INSTALL_ACTION)
            addAction(UninstallService.APP_UNINSTALL_ACTION)
        })
    }

    override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(installBroadcastReceiver)
        workManager.cancelWorkById(patcherWorker.id)

        outputFile.delete()
        signedFile.delete()
    }

    private fun signApk(): Boolean {
        if (!hasSigned) {
            try {
                signerService.createSigner().signApk(outputFile, signedFile)
            } catch (e: Throwable) {
                e.printStackTrace()
                app.toast(app.getString(R.string.sign_fail, e::class.simpleName))
                return false
            }
        }

        return true
    }

    fun export(uri: Uri?) = uri?.let {
        if (signApk()) {
            Files.copy(signedFile.toPath(), app.contentResolver.openOutputStream(it))
            app.toast(app.getString(R.string.export_app_success))
        }
    }

    fun installOrOpen() {
        installedPackageName?.let {
            pm.launch(it)
            return
        }

        isInstalling = true
        try {
            if (!signApk()) return
            pm.installApp(listOf(signedFile))
        } finally {
            isInstalling = false
        }
    }


    data class PatcherState(val status: Boolean?, val stepGroups: List<StepGroup>)
}