package app.revanced.manager.util

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

private const val byteArraySize = 1024 * 1024 // Because 1,048,576 is not readable

@Immutable
@Parcelize
data class AppInfo(
    val packageName: String,
    val patches: Int?,
    val packageInfo: PackageInfo?,
    val path: File? = null
) : Parcelable

@SuppressLint("QueryPermissionsNeeded")
@Suppress("DEPRECATION")
class PM(
    private val app: Application,
    sourceRepository: SourceRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val appList = sourceRepository.bundles.map { bundles ->
        val compatibleApps = scope.async {
            val compatiblePackages = bundles.values
                .flatMap { it.patches }
                .flatMap { it.compatiblePackages.orEmpty() }
                .groupingBy { it.packageName }
                .eachCount()

            compatiblePackages.keys.map { pkg ->
                try {
                    val packageInfo = app.packageManager.getPackageInfo(pkg, 0)
                    AppInfo(
                        pkg,
                        compatiblePackages[pkg],
                        packageInfo,
                        File(packageInfo.applicationInfo.sourceDir)
                    )
                } catch (e: NameNotFoundException) {
                    AppInfo(
                        pkg,
                        compatiblePackages[pkg],
                        null
                    )
                }
            }
        }

        val installedApps = scope.async {
            app.packageManager.getInstalledPackages(MATCH_UNINSTALLED_PACKAGES).map { packageInfo ->
                AppInfo(
                    packageInfo.packageName,
                    0,
                    packageInfo,
                    File(packageInfo.applicationInfo.sourceDir)
                )
            }
        }

        if (compatibleApps.await().isNotEmpty()) {
            (compatibleApps.await() + installedApps.await())
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<AppInfo> {
                        it.patches
                    }.thenBy { it.packageInfo?.applicationInfo?.loadLabel(app.packageManager).toString() }.thenBy { it.packageName }
                )
        } else { emptyList() }
    }.flowOn(Dispatchers.IO)

    fun getPackageInfo(packageName: String): PackageInfo? =
        try {
            app.packageManager.getPackageInfo(packageName, 0)
        } catch (e: NameNotFoundException) {
            null
        }

    suspend fun installApp(apks: List<File>) = withContext(Dispatchers.IO) {
        val packageInstaller = app.packageManager.packageInstaller
        packageInstaller.openSession(packageInstaller.createSession(sessionParams)).use { session ->
            apks.forEach { apk -> session.writeApk(apk) }
            session.commit(app.installIntentSender)
        }
    }

    fun uninstallPackage(pkg: String) {
        val packageInstaller = app.packageManager.packageInstaller
        packageInstaller.uninstall(pkg, app.uninstallIntentSender)
    }

    fun launch(pkg: String) = app.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(it)
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