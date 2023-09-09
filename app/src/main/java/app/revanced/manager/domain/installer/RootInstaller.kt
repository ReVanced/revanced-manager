package app.revanced.manager.domain.installer

import android.app.Application
import app.revanced.manager.service.RootConnection
import app.revanced.manager.util.PM
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootInstaller(
    private val app: Application,
    private val rootConnection: RootConnection,
    private val pm: PM
) {
    fun hasRootAccess() = Shell.isAppGrantedRoot() ?: false

    fun isAppInstalled(packageName: String) =
        rootConnection.remoteFS?.getFile("$modulesPath/$packageName-revanced")
            ?.exists() ?: throw RootServiceException()

    fun isAppMounted(packageName: String): Boolean {
        return pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir?.let {
            Shell.cmd("mount | grep \"$it\"").exec().isSuccess
        } ?: false
    }

    fun mount(packageName: String) {
        if (isAppMounted(packageName)) return

        val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
            ?: throw Exception("Failed to load application info")
        val patchedAPK = "$modulesPath/$packageName-revanced/$packageName.apk"

        Shell.cmd("mount -o bind \"$patchedAPK\" \"$stockAPK\"").exec()
            .also { if (!it.isSuccess) throw Exception("Failed to mount APK") }
    }

    fun unmount(packageName: String) {
        if (!isAppMounted(packageName)) return

        val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
            ?: throw Exception("Failed to load application info")

        Shell.cmd("umount -l \"$stockAPK\"").exec()
            .also { if (!it.isSuccess) throw Exception("Failed to unmount APK") }
    }

    suspend fun install(
        patchedAPK: File,
        stockAPK: File?,
        packageName: String,
        version: String,
        label: String
    ) {
        withContext(Dispatchers.IO) {
            rootConnection.remoteFS?.let { remoteFS ->
                val assets = app.assets
                val modulePath = "$modulesPath/$packageName-revanced"

                unmount(packageName)

                stockAPK?.let { stockApp ->
                    pm.getPackageInfo(packageName)?.let { packageInfo ->
                        if (packageInfo.versionName <= version)
                            Shell.cmd("pm uninstall -k --user 0 $packageName").exec()
                                .also { if (!it.isSuccess) throw Exception("Failed to uninstall stock app") }
                    }

                    Shell.cmd("pm install \"${stockApp.absolutePath}\"").exec()
                        .also { if (!it.isSuccess) throw Exception("Failed to install stock app") }
                }

                remoteFS.getFile(modulePath).mkdir()

                listOf(
                    "service.sh",
                    "module.prop",
                ).forEach { file ->
                    assets.open("root/$file").use { inputStream ->
                        remoteFS.getFile("$modulePath/$file").newOutputStream()
                            .use { outputStream ->
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

                    Shell.cmd(
                        "chmod 644 $apkPath",
                        "chown system:system $apkPath",
                        "chcon u:object_r:apk_data_file:s0 $apkPath",
                        "chmod +x $modulePath/service.sh"
                    ).exec()
                        .let { if (!it.isSuccess) throw Exception("Failed to set file permissions") }
                }
            } ?: throw RootServiceException()
        }
    }

    fun uninstall(packageName: String) {
        rootConnection.remoteFS?.let { remoteFS ->
            if (isAppMounted(packageName))
                unmount(packageName)

            remoteFS.getFile("$modulesPath/$packageName-revanced").deleteRecursively()
                .also { if (!it) throw Exception("Failed to delete files") }
        } ?: throw RootServiceException()
    }

    companion object {
        const val modulesPath = "/data/adb/modules"
    }
}

class RootServiceException: Exception("Root not available")