package app.revanced.manager.backend.vital

import android.content.pm.PackageManager

fun PackageManager.pathFromPackageName(packageName: String) =
    getApplicationInfo(packageName, 0).publicSourceDir



