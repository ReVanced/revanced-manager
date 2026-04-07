package app.revanced.manager.domain.installer

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.revanced.library.MagiskUtils
import app.revanced.library.ShellCommandException
import app.revanced.manager.IRootSystemService
import app.revanced.manager.service.ManagerRootService
import app.revanced.manager.util.PM
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.File
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext

class RootInstaller(
    private val app: Application,
    private val pm: PM
) : ServiceConnection {
    private var remoteFS = CompletableDeferred<FileSystemManager>()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val ipc = IRootSystemService.Stub.asInterface(service)
        val binder = ipc.fileSystemService

        remoteFS.complete(FileSystemManager.getRemote(binder))
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        remoteFS = CompletableDeferred()
    }

    private suspend fun awaitRemoteFS(): FileSystemManager {
        if (remoteFS.isActive) {
            withContext(Dispatchers.Main) {
                val intent = Intent(app, ManagerRootService::class.java)
                RootService.bind(intent, this@RootInstaller)
            }
        }

        return withTimeoutOrNull(Duration.ofSeconds(20L)) { remoteFS.await() }
                ?: throw RootServiceException()
    }

    private suspend fun getShell() = with(CompletableDeferred<Shell>()) {
        Shell.getShell(::complete)

        await()
    }

    suspend fun execute(vararg commands: String): Shell.Result {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        return getShell().newJob().add(*commands).to(stdout, stderr).exec()
    }

    fun hasRootAccess() = MagiskUtils.hasRootAccess()

    fun isDeviceRooted() = MagiskUtils.isDeviceRooted()

    suspend fun isAppInstalled(packageName: String) =
            MagiskUtils.isInstalled(packageName, awaitRemoteFS())

    suspend fun isAppInstalledAsMagiskModule(packageName: String) =
            MagiskUtils.isInstalledAsMagiskModule(packageName, awaitRemoteFS())

    suspend fun isAppMounted(packageName: String) = withContext(Dispatchers.IO) {
        pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir?.let {
            execute("mount | grep \"$it\"").isSuccess
        } ?: false
    }

    suspend fun mount(packageName: String) {
        if (isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val sourceDir =
                    pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                            ?: throw Exception("Failed to load application info")
            
            MagiskUtils.mount(packageName, sourceDir)
        }
    }

    suspend fun unmount(packageName: String) {
        if (!isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val sourceDir = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                ?: throw Exception("Failed to load application info")

            MagiskUtils.unmount(sourceDir)
        }
    }

    suspend fun install(
        patchedAPK: File,
        stockAPK: File?,
        packageName: String,
        version: String,
        label: String
    ) = withContext(Dispatchers.IO) {
        val remoteFS = awaitRemoteFS()

        unmount(packageName)

        stockAPK?.let { stockApp ->
            execute("pm uninstall -k --user 0 $packageName")
            val result = execute("pm install -r -d --user 0 \"${stockApp.absolutePath}\"")
            if (!result.isSuccess) {
                throw ShellCommandException("Failed to install stock app", result.code, result.out, result.err)
            }
            stockApp.delete()
        }

        MagiskUtils.provisionRootFolder(remoteFS, app.assets, packageName, version, label, patchedAPK)
    }

    suspend fun installAsMagiskModule(
        patchedAPK: File,
        packageName: String,
        version: String,
        label: String
    ) = withContext(Dispatchers.IO) {
        MagiskUtils.provisionMagiskModule(awaitRemoteFS(), app.assets, packageName, version, label, patchedAPK)
    }

    suspend fun uninstallMagiskModule(packageName: String) {
        MagiskUtils.uninstallMagiskModule(packageName, awaitRemoteFS())
    }

    suspend fun uninstall(packageName: String) {
        if (isAppMounted(packageName)) unmount(packageName)
        MagiskUtils.uninstall(packageName, awaitRemoteFS())
    }
}

class RootServiceException : Exception("Root not available")