package app.revanced.manager.compose.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import app.revanced.manager.compose.service.InstallService
import app.revanced.manager.compose.service.UninstallService
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.File

private const val byteArraySize = 1024 * 1024 // Because 1,048,576 is not readable

@Parcelize
data class PackageInfo(val packageName: String, val version: String, val apk: File) : Parcelable {
    constructor(appInfo: PM.AppInfo) : this(appInfo.packageName, appInfo.versionName, appInfo.apk)
}

@SuppressLint("QueryPermissionsNeeded")
@Suppress("DEPRECATION")
object PM {
    val appList = mutableStateListOf<AppInfo>()
    val supportedAppList = mutableStateListOf<AppInfo>()

    suspend fun loadApps(context: Context) {
        val packageManager = context.packageManager

        val localAppList = mutableListOf<AppInfo>()

        packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map {
            AppInfo(
                it.packageName,
                "0.69.420",
                it.loadLabel(packageManager).toString(),
                it.loadIcon(packageManager),
                File("h")
            )
        }.also { localAppList.addAll(it) }.also { supportedAppList.addAll(it) }
    }

    @Parcelize
    data class AppInfo(
        val packageName: String,
        val versionName: String,
        val label: String,
        val icon: @RawValue Drawable? = null,
        val apk: File,
    ) : Parcelable

    fun installApp(apks: List<File>, context: Context) {
        val packageInstaller = context.packageManager.packageInstaller
        packageInstaller.openSession(packageInstaller.createSession(sessionParams)).use { session ->
            apks.forEach { apk -> session.writeApk(apk) }
            session.commit(context.installIntentSender)
        }
    }

    fun uninstallPackage(pkg: String, context: Context) {
        val packageInstaller = context.packageManager.packageInstaller
        packageInstaller.uninstall(pkg, context.uninstallIntentSender)
    }

    fun getApkInfo(apk: File, context: Context) = context.packageManager.getPackageArchiveInfo(apk.path, 0)!!.let { PackageInfo(it.packageName, it.versionName, apk) }
}

private fun PackageInstaller.Session.writeApk(apk: File) {
    apk.inputStream().use { inputStream ->
        openWrite(apk.name, 0, apk.length()).use { outputStream ->
            inputStream.copyTo(outputStream, byteArraySize)
            fsync(outputStream)
        }
    }
}

private val intentFlags
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        PendingIntent.FLAG_MUTABLE
    else
        0

private val sessionParams
    get() = PackageInstaller.SessionParams(
        PackageInstaller.SessionParams.MODE_FULL_INSTALL
    ).apply {
        setInstallReason(PackageManager.INSTALL_REASON_USER)
    }

private val Context.installIntentSender
    get() = PendingIntent.getService(
        this,
        0,
        Intent(this, InstallService::class.java),
        intentFlags
    ).intentSender

private val Context.uninstallIntentSender
    get() = PendingIntent.getService(
        this,
        0,
        Intent(this, UninstallService::class.java),
        intentFlags
    ).intentSender