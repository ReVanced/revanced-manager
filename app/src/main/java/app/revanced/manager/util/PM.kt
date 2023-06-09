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
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.service.InstallService
import app.revanced.manager.service.UninstallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

private const val byteArraySize = 1024 * 1024 // Because 1,048,576 is not readable

@Immutable
@Parcelize
data class AppInfo(
    val packageName: String,
    val patches: Int,
    val packageInfo: PackageInfo?,
    val path: File? = null
) : Parcelable

@SuppressLint("QueryPermissionsNeeded")
@Suppress("DEPRECATION")
class PM(
    private val app: Application,
    private val sourceRepository: SourceRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val installedApps = MutableStateFlow(emptyList<AppInfo>())
    private val compatibleApps = MutableStateFlow(emptyList<AppInfo>())

    val appList: StateFlow<List<AppInfo>> = compatibleApps.combine(installedApps) { compatibleApps, installedApps ->
        if (compatibleApps.isNotEmpty()) {
            (compatibleApps + installedApps)
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<AppInfo> {
                        it.patches
                    }.thenBy { it.packageInfo?.applicationInfo?.loadLabel(app.packageManager).toString() }.thenBy { it.packageName }
                )
        } else {
            emptyList()
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    suspend fun getCompatibleApps() {
        sourceRepository.bundles.collect { bundles ->
            val compatiblePackages = HashMap<String, Int>()

            bundles.flatMap { it.value.patches }.forEach {
                it.compatiblePackages?.forEach { pkg ->
                    compatiblePackages[pkg.name] = compatiblePackages.getOrDefault(pkg.name, 0) + 1
                }
            }

            withContext(Dispatchers.IO) {
                compatibleApps.emit(
                    compatiblePackages.keys.map { pkg ->
                        try {
                            val packageInfo = app.packageManager.getPackageInfo(pkg, 0)
                            AppInfo(
                                pkg,
                                compatiblePackages[pkg] ?: 0,
                                packageInfo,
                                File(packageInfo.applicationInfo.sourceDir)
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            AppInfo(
                                pkg,
                                compatiblePackages[pkg] ?: 0,
                                null
                            )
                        }
                    }
                )
            }
        }
    }

    suspend fun getInstalledApps() {
        installedApps.emit(app.packageManager.getInstalledPackages(MATCH_UNINSTALLED_PACKAGES).map { packageInfo ->
            AppInfo(
                packageInfo.packageName,
                0,
                packageInfo,
                File(packageInfo.applicationInfo.sourceDir)
            )
        })
    }

    fun installApp(apks: List<File>) {
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

    fun getApkInfo(apk: File) = app.packageManager.getPackageArchiveInfo(apk.path, 0)!!.let {
        AppInfo(
            it.packageName,
            0,
            it,
            apk
        )
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