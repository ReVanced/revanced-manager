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
        apk: File,
        packageName: String,
        version: String,
        label: String
    ) {
        withContext(Dispatchers.IO) {
            rootConnection.remoteFS?.let { remoteFS ->
                val assets = app.assets
                val modulePath = "$modulesPath/$packageName-revanced"

                unmount(packageName)

                remoteFS.getFile(modulePath).mkdir()

                listOf(
                    "service.sh",
                    "module.prop",
                ).forEach { file ->
                    assets.open("root/$file").use { inputStream ->
                        remoteFS.getFile("$modulePath/$file").newOutputStream()
                            .use { outputStream ->
                                val buffer = ByteArray(1024)
                                var bytesRead: Int

                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    val content = String(buffer, 0, bytesRead)
                                        .replace("__PKG_NAME__", packageName)
                                        .replace("__VERSION__", version)
                                        .replace("__LABEL__", label)
                                        .toByteArray()

                                    outputStream.write(content)
                                }
                            }
                    }
                }

                "$modulePath/$packageName.apk".let { patchedAPK ->

                    remoteFS.getFile(apk.absolutePath)
                        .also { if (!it.exists()) throw Exception("File doesn't exist") }
                        .newInputStream().use { inputStream ->
                        remoteFS.getFile(patchedAPK).newOutputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Shell.cmd(
                        "chmod 644 $patchedAPK",
                        "chown system:system $patchedAPK",
                        "chcon u:object_r:apk_data_file:s0 $patchedAPK",
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

        /*val isDeviceRooted =
            try {
                Runtime.getRuntime().exec("su --version")
                true
            } catch (_: IOException) {
                false
            }*/
    }
}

class RootServiceException: Exception("Root not available")