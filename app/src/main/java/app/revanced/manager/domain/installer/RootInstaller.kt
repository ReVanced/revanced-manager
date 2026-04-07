package app.revanced.manager.domain.installer

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.revanced.manager.IRootSystemService
import app.revanced.manager.service.ManagerRootService
import app.revanced.manager.util.PM
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration

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

        return withTimeoutOrNull(Duration.ofSeconds(20L)) {
            remoteFS.await()
        } ?: throw RootServiceException()
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

    fun hasRootAccess() = Shell.isAppGrantedRoot() ?: false

    fun isDeviceRooted() = System.getenv("PATH")?.split(":")?.any { path ->
        File(path, "su").canExecute()
    } ?: false

    suspend fun isAppInstalled(packageName: String) =
        awaitRemoteFS().getFile("$modulesPath/$packageName-revanced").exists()

    suspend fun isAppInstalledAsMagiskModule(packageName: String) =
        awaitRemoteFS().getFile("$modulesPath/revanced_${packageName.replace('.', '_')}").exists()

    suspend fun isAppMounted(packageName: String) = withContext(Dispatchers.IO) {
        pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir?.let {
            execute("mount | grep \"$it\"").isSuccess
        } ?: false
    }

    suspend fun mount(packageName: String) {
        if (isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                ?: throw Exception("Failed to load application info")
            val patchedAPK = "$modulesPath/$packageName-revanced/$packageName.apk"

            execute("mount -o bind \"$patchedAPK\" \"$stockAPK\"").assertSuccess("Failed to mount APK")
        }
    }

    suspend fun unmount(packageName: String) {
        if (!isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                ?: throw Exception("Failed to load application info")

            execute("umount -l \"$stockAPK\"").assertSuccess("Failed to unmount APK")
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
        val assets = app.assets
        val modulePath = "$modulesPath/$packageName-revanced"

        unmount(packageName)

        stockAPK?.let { stockApp ->
            // TODO: get user id programmatically
            execute("pm uninstall -k --user 0 $packageName")

            execute("pm install -r -d --user 0 \"${stockApp.absolutePath}\"")
                .assertSuccess("Failed to install stock app")

            stockApp.delete()
        }

        remoteFS.getFile(modulePath).apply {
            if (!mkdirs() && !exists()) {
                throw Exception("Failed to create module directory")
            }
        }

        listOf(
            "service.sh",
            "module.prop",
        ).forEach { file ->
            assets.open("root/$file").use { inputStream ->
                remoteFS.getFile("$modulePath/$file").newOutputStream().use { outputStream ->
                    val content = String(inputStream.readBytes())
                        .replace("__PKG_NAME__", packageName)
                        .replace("__VERSION__", version)
                        .replace("__LABEL__", label)
                        .toByteArray()

                    outputStream.write(content)
                }
            }
        }

        "$modulePath/$packageName.apk".let { apkPath ->
            remoteFS.getFile(patchedAPK.absolutePath)
                .also { if (!it.exists()) throw Exception("File doesn't exist") }
                .newInputStream().use { inputStream ->
                    remoteFS.getFile(apkPath).newOutputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

            execute(
                "chmod 644 $apkPath",
                "chown system:system $apkPath",
                "chcon u:object_r:apk_data_file:s0 $apkPath",
                "chmod +x $modulePath/service.sh"
            ).assertSuccess("Failed to set file permissions")
        }
    }

    suspend fun installAsMagiskModule(
        patchedAPK: File,
        packageName: String,
        version: String,
        label: String
    ) = withContext(Dispatchers.IO) {
        val remoteFS = awaitRemoteFS()
        val sanitizedPackageName = packageName.replace('.', '_')
        val modulePath = "$modulesPath/revanced_$sanitizedPackageName"
        val systemAppPath = "$modulePath/system/app/$sanitizedPackageName"
        val assets = app.assets

        execute("mkdir -p \"$systemAppPath\"").assertSuccess("Failed to create system app directory")

        val moduleProp = buildString {
            appendLine("id=revanced_$sanitizedPackageName")
            appendLine("name=$label ReVanced")
            appendLine("version=$version")
            appendLine("versionCode=1")
            appendLine("author=ReVanced")
            append("description=Patched by ReVanced")
        }
        remoteFS.getFile("$modulePath/module.prop").newOutputStream().use { outputStream ->
            outputStream.write(moduleProp.toByteArray())
        }

        assets.open("root/service.sh").use { inputStream ->
            remoteFS.getFile("$modulePath/service.sh").newOutputStream()
                .use { outputStream ->
                    val content = String(inputStream.readBytes())
                        .replace("__PKG_NAME__", packageName)
                        .replace("__VERSION__", version)
                        .replace("__LABEL__", label)
                        .toByteArray()

                    outputStream.write(content)
                }
        }

        val targetApkPath = "$systemAppPath/base.apk"
        remoteFS.getFile(patchedAPK.absolutePath)
            .also { if (!it.exists()) throw Exception("File doesn't exist") }
            .newInputStream().use { inputStream ->
                remoteFS.getFile(targetApkPath).newOutputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

        execute(
            "chmod 644 \"$targetApkPath\"",
            "chmod 755 \"$systemAppPath\"",
            "chown -R system:system \"$modulePath/system\"",
            "chcon -R u:object_r:system_file:s0 \"$modulePath/system\"",
            "chmod +x \"$modulePath/service.sh\""
        ).assertSuccess("Failed to set file permissions")
    }

    suspend fun uninstallMagiskModule(packageName: String) {
        val remoteFS = awaitRemoteFS()
        val sanitizedPackageName = packageName.replace('.', '_')

        remoteFS.getFile("$modulesPath/revanced_$sanitizedPackageName").deleteRecursively()
            .also { if (!it) throw Exception("Failed to delete Magisk module files") }
    }

    suspend fun uninstall(packageName: String) {
        val remoteFS = awaitRemoteFS()
        if (isAppMounted(packageName))
            unmount(packageName)

        remoteFS.getFile("$modulesPath/$packageName-revanced").deleteRecursively()
            .also { if (!it) throw Exception("Failed to delete files") }
    }

    companion object {
        const val modulesPath = "/data/adb/modules"

        private fun Shell.Result.assertSuccess(errorMessage: String) {
            if (!isSuccess) {
                throw ShellCommandException(
                    errorMessage,
                    code,
                    out,
                    err
                )
            }
        }
    }
}

class ShellCommandException(
    val userMessage: String,
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) : Exception(format(userMessage, exitCode, stdout, stderr)) {
    companion object {
        private fun format(
            message: String,
            exitCode: Int,
            stdout: List<String>,
            stderr: List<String>
        ): String =
            buildString {
                appendLine(message)
                appendLine("Exit code: $exitCode")

                val output = stdout.filter { it.isNotBlank() }
                val errors = stderr.filter { it.isNotBlank() }

                if (output.isNotEmpty()) {
                    appendLine("stdout:")
                    output.forEach(::appendLine)
                }
                if (errors.isNotEmpty()) {
                    appendLine("stderr:")
                    errors.forEach(::appendLine)
                }
            }
    }
}

class RootServiceException : Exception("Root not available")