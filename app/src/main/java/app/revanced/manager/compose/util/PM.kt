package app.revanced.manager.compose.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import app.revanced.manager.compose.service.InstallService
import app.revanced.manager.compose.service.UninstallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.File

private const val byteArraySize = 1024 * 1024 // Because 1,048,576 is not readable

@SuppressLint("QueryPermissionsNeeded")
@Suppress("DEPRECATION")
object PM {

    val testList = mapOf(
        "com.google.android.youtube" to 59,
        "com.android.vending" to 34,
        "com.backdrops.wallpapers" to 2,
        "com.termux" to 2,
        "com.notinstalled.app" to 1,
        "com.2notinstalled.app" to 1,
        "org.adaway" to 5,
        "com.activitymanager" to 1,
        "com.guoshi.httpcanary" to 1,
        "org.lsposed.lspatch" to 1,
        "app.revanced.manager.flutter" to 100,
        "com.reddit.frontpage" to 20
    )

    val appList = mutableStateListOf<AppInfo>()
    val supportedAppList = mutableStateListOf<AppInfo>()

    suspend fun loadApps(context: Context) {
        val packageManager = context.packageManager

        testList.keys.map {
            try {
                val applicationInfo = packageManager.getApplicationInfo(it, 0)

                AppInfo(
                    it,
                    applicationInfo.loadLabel(packageManager).toString(),
                    applicationInfo.loadIcon(packageManager),
                )
            } catch (e: PackageManager.NameNotFoundException) {
                AppInfo(
                    it,
                    "Not installed"
                )
            }
        }.let { list ->
            list.sortedWith(
                compareByDescending<AppInfo> {
                    testList[it.packageName]
                }.thenBy { it.label }.thenBy { it.packageName }
            )
        }.also {
            withContext(Dispatchers.Main) { supportedAppList.addAll(it) }
        }

        val localAppList = mutableListOf<AppInfo>()

        packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map {
            AppInfo(
                it.packageName,
                it.loadLabel(packageManager).toString(),
                it.loadIcon(packageManager)
            )
        }.also { localAppList.addAll(it) }

        testList.keys.mapNotNull { packageName ->
            if (!localAppList.any { packageName == it.packageName }) {
                AppInfo(
                    packageName,
                    "Not installed"
                )
            } else {
                null
            }
        }.also { localAppList.addAll(it) }

        localAppList.sortWith(
            compareByDescending<AppInfo> {
                testList[it.packageName]
            }.thenBy { it.label }.thenBy { it.packageName }
        ).also {
            withContext(Dispatchers.Main) { appList.addAll(localAppList) }
        }
    }

    @Parcelize
    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: @RawValue Drawable? = null
    ) : Parcelable

    fun installApp(apk: File, context: Context) {
        val packageInstaller = context.packageManager.packageInstaller
        val session =
            packageInstaller.openSession(packageInstaller.createSession(sessionParams))
        session.writeApk(apk)
        session.commit(context.installIntentSender)
        session.close()
    }

    fun uninstallPackage(pkg: String, context: Context) {
        val packageInstaller = context.packageManager.packageInstaller
        packageInstaller.uninstall(pkg, context.uninstallIntentSender)
    }
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