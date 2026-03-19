package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.data.room.apps.installed.InstalledPatchBundle
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.simpleMessage
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure

class InstalledAppInfoViewModel(
    packageName: String
) : ViewModel(), KoinComponent {
    private val context: Application by inject()
    private val pm: PM by inject()
    private val installedAppRepository: InstalledAppRepository by inject()
    val rootInstaller: RootInstaller by inject()

    lateinit var onBackClick: () -> Unit

    var installedApp: InstalledApp? by mutableStateOf(null)
        private set
    var appInfo: PackageInfo? by mutableStateOf(null)
        private set
    var appliedPatches: PatchSelection? by mutableStateOf(null)
    var patchBundles: List<InstalledPatchBundle> by mutableStateOf(emptyList())
        private set
    var isMounted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            installedApp = installedAppRepository.get(packageName)?.also {
                isMounted = rootInstaller.isAppMounted(it.currentPackageName)
                appInfo = withContext(Dispatchers.IO) {
                    pm.getPackageInfo(it.currentPackageName)
                }
                appliedPatches = withContext(Dispatchers.IO) {
                    installedAppRepository.getAppliedPatches(it.currentPackageName)
                }
                patchBundles = withContext(Dispatchers.IO) {
                    installedAppRepository.getInstalledPatchBundles(it.currentPackageName)
                }
            }
        }
    }

    fun launch() = installedApp?.currentPackageName?.let(pm::launch)

    fun mountOrUnmount() = viewModelScope.launch {
        val pkgName = installedApp?.currentPackageName ?: return@launch
        try {
            if (isMounted)
                rootInstaller.unmount(pkgName)
            else
                rootInstaller.mount(pkgName)
        } catch (e: Exception) {
            if (isMounted) {
                context.toast(context.getString(R.string.failed_to_unmount, e.simpleMessage()))
                Log.e(tag, "Failed to unmount", e)
            } else {
                context.toast(context.getString(R.string.failed_to_mount, e.simpleMessage()))
                Log.e(tag, "Failed to mount", e)
            }
        } finally {
            isMounted = rootInstaller.isAppMounted(pkgName)
        }
    }

    fun uninstall() {
        val app = installedApp ?: return
        viewModelScope.launch {
            when (app.installType) {
                InstallType.DEFAULT, InstallType.SHIZUKU -> {
                    when (val result = pm.uninstallPackage(app.currentPackageName)) {
                        is Session.State.Failed<UninstallFailure> -> {
                            if (result.failure !is UninstallFailure.Aborted) {
                                val msg = result.failure.message.orEmpty()
                                context.toast(
                                    this@InstalledAppInfoViewModel.context.getString(
                                        R.string.uninstall_app_fail,
                                        msg
                                    )
                                )
                            }
                            return@launch
                        }
                        Session.State.Succeeded -> {}
                    }
                }

                InstallType.MOUNT -> rootInstaller.uninstall(app.currentPackageName)
            }
            installedAppRepository.delete(app)
            onBackClick()
        }
    }
}