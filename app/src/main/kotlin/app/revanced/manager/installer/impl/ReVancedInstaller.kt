package app.revanced.manager.installer.impl

import app.revanced.manager.installer.Preference
import app.revanced.manager.installer.base.AppInstaller
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.File

class ReVancedInstaller : AppInstaller() {
    override fun install(preference: Preference) {

    }

    override fun installRoot(preference: Preference) {
        // get apk path from package manager
        var stockPath = Shell.cmd("pm path com.google.android.youtube").exec().out.firstOrNull()
        if (stockPath != null) {
            stockPath = stockPath.split(':')[1]
        }

        val stockFile: File = if (!preference.useInstalled) {
            File("someFile");
        } else {
            File(stockPath)
        }
        // apk 2 jar

        // merge with integrations jar

        // run patcher

        // create apk
        val patchedFile = File("")

        // post

        // save ReVanced to adb
        val basePath = "/data/adb/revanced/base.apk"

        Shell.cmd("mkdir -p /data/adb/revanced").exec()
        SuFile.open(basePath)
            .writeBytes(patchedFile.readBytes())
        Shell.cmd(
            "chmod 655 $basePath",
            "chown system:system $basePath",
            "chcon u:object_r:apk_data_file:s0 $basePath"
        ).exec()

        // create post-fs script
        createWriteSuFile(
            "/data/adb/post-fs-data.d/revanced.sh",
            "while read line; do echo \${line} | grep com.google.android.youtube | awk '{print \\$2}' | xargs umount -l; done< /proc/mounts"
        )

        // create service.d script
        createWriteSuFile(
            "/data/adb/service.d/revanced.sh",
            """
                    #!/system/bin/sh
                    while [ "$(getprop sys.boot_completed | tr -d '\r')" != "1" ]; do sleep 1; done
                    sleep 1
                    chcon u:object_r:apk_data_file:s0 $basePath
                    mount -o bind $basePath $stockPath
                """.trimIndent()
        )

        // mount ReVanced over stock
        Shell.cmd("mount -o bind $basePath $stockPath").exec()
    }

    private fun createWriteSuFile(path: String, text: String) {
        val postFsFile = SuFile.open(path)
        postFsFile.createNewFile()
        postFsFile.writeText(text)
    }
}