package app.revanced.manager.util

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.NameNotFoundException
import androidx.core.content.pm.PackageInfoCompat
import android.content.pm.Signature
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.revanced.manager.domain.repository.PatchBundleRepository
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
    val packageInfo: PackageInfo?
) : Parcelable

@SuppressLint("QueryPermissionsNeeded")
class PM(
    private val app: Application,
    patchBundleRepository: PatchBundleRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val appList = patchBundleRepository.bundles.map { bundles ->
        val compatibleApps = scope.async {
            val compatiblePackages = bundles.values
                .flatMap { it.patches }
                .flatMap { it.compatiblePackages.orEmpty() }
                .groupingBy { it.packageName }
                .eachCount()

            compatiblePackages.keys.map { pkg ->
                getPackageInfo(pkg)?.let { packageInfo ->
                    AppInfo(
                        pkg,
                        compatiblePackages[pkg],
                        packageInfo
                    )
                } ?: AppInfo(
                    pkg,
                    compatiblePackages[pkg],
                    null
                )
            }
        }

        val installedApps = scope.async {
            getInstalledPackages().map { packageInfo ->
                AppInfo(
                    packageInfo.packageName,
                    0,
                    packageInfo
                )
            }
        }

        if (compatibleApps.await().isNotEmpty()) {
            (compatibleApps.await() + installedApps.await())
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<AppInfo> {
                        it.packageInfo != null && (it.patches ?: 0) > 0
                    }.thenByDescending {
                        it.patches
                    }.thenBy {
                        it.packageInfo?.label()
                    }.thenBy { it.packageName }
                )
        } else {
            emptyList()
        }
    }.flowOn(Dispatchers.IO)

    private fun getInstalledPackages(flags: Int = 0): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            app.packageManager.getInstalledPackages(PackageInfoFlags.of(flags.toLong()))
        else
            app.packageManager.getInstalledPackages(flags)

    fun getPackagesWithFeature(feature: String) =
        getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
            .filter { pkg ->
                pkg.reqFeatures?.any { it.name == feature } ?: false
            }

    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                app.packageManager.getPackageInfo(packageName, PackageInfoFlags.of(flags.toLong()))
            else
                app.packageManager.getPackageInfo(packageName, flags)
        } catch (_: NameNotFoundException) {
            null
        }

    fun getPackageInfo(file: File): PackageInfo? {
        val path = file.absolutePath
        val pkgInfo = app.packageManager.getPackageArchiveInfo(path, 0) ?: return null

        // This is needed in order to load label and icon.
        pkgInfo.applicationInfo!!.apply {
            sourceDir = path
            publicSourceDir = path
        }

        return pkgInfo
    }

    fun PackageInfo.label() = this.applicationInfo!!.loadLabel(app.packageManager).toString()

    fun getVersionCode(packageInfo: PackageInfo) = PackageInfoCompat.getLongVersionCode(packageInfo)

    fun getSignature(packageName: String): Signature =
        // Get the last signature from the list because we want the newest one if SigningInfo.getSigningCertificateHistory() was used.
        PackageInfoCompat.getSignatures(app.packageManager, packageName).last()

    @SuppressLint("InlinedApi")
    fun hasSignature(packageName: String, signature: ByteArray) = PackageInfoCompat.hasSignatures(
        app.packageManager,
        packageName,
        mapOf(signature to PackageManager.CERT_INPUT_RAW_X509),
        false
    )

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

    fun canInstallPackages() = app.packageManager.canRequestPackageInstalls()

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                setRequestUpdateOwnership(true)
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
}
