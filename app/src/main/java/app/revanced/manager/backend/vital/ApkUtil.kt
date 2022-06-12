package app.revanced.manager.backend.vital

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.TypedArray
import com.topjohnwu.superuser.Shell

class ApkUtil() {
    lateinit var pm: PackageManager;

    constructor(packageManager: PackageManager) : this() {
        pm = packageManager
    }

    fun getInstalledApplications(): Array<ApplicationInfo> {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).toTypedArray()
    }

    fun pathFromPackageName(packageName: String): String {
        return pm.getApplicationInfo(packageName, 0).publicSourceDir
    }

    fun installFromPath(path: String) {
        if (Shell.rootAccess())
            Shell.cmd("pm install ${path}")
        else {

        }
    }

    @Deprecated("does not utilize mount scripts")
    fun legacyInstaller(path: String) {
        if (Shell.rootAccess())
            Shell.cmd("pm install ${path}")
    }
}

