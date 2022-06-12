package app.revanced.manager.backend.vital

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.TypedArray
import com.topjohnwu.superuser.Shell
import java.io.File

class ApkUtil() {
    lateinit var cn: Context;
    lateinit var pm: PackageManager;

    constructor(context: Context) : this() {
        cn = context
        pm = context.packageManager
    }

    fun getInstalledApplications(): Array<ApplicationInfo> {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).toTypedArray()
    }

    fun pathFromPackageName(packageName: String): String {
        return pm.getApplicationInfo(packageName, 0).publicSourceDir
    }

    fun getCacheDir(): File {
        return cn.cacheDir
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

