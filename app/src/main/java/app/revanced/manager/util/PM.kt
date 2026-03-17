package app.revanced.manager.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.NameNotFoundException
import androidx.core.content.pm.PackageInfoCompat
import android.content.pm.Signature
import android.os.Build
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.revanced.manager.domain.repository.PatchBundleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParametersDsl
import java.io.File

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
    patchBundleRepository: PatchBundleRepository,
    private val uninstaller: PackageUninstaller
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val appList = patchBundleRepository.bundleInfoFlow.map { bundles ->
        val compatibleApps = scope.async {
            val compatiblePackages = bundles
                .flatMap { (_, bundle) -> bundle.patches }
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

    @SuppressLint("InlinedApi")
    fun getApkSignature(file: File): Signature? {
        val path = file.absolutePath
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            app.packageManager.getPackageArchiveInfo(
                path,
                PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        else
            app.packageManager.getPackageArchiveInfo(
                path,
                PackageManager.GET_SIGNING_CERTIFICATES
            )

        return pkgInfo?.signingInfo?.let { signingInfo ->
            if (signingInfo.hasMultipleSigners()) {
                val managerSignature = getManagerSignature()
                signingInfo.apkContentsSigners.firstOrNull { it == managerSignature }
                    ?: signingInfo.apkContentsSigners.lastOrNull()
            } else {
                signingInfo.signingCertificateHistory.lastOrNull()
            }
        }
    }

    fun getManagerSignature(): Signature = getSignature(app.packageName)

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

    suspend fun uninstallPackage(pkg: String, config: UninstallParametersDsl.() -> Unit = {}) = withContext(Dispatchers.IO) {
        uninstaller.createSession(pkg) {
            confirmation = Confirmation.IMMEDIATE
            config()
        }.await()
    }

    fun launch(pkg: String) = app.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(it)
    }

    fun canInstallPackages() = app.packageManager.canRequestPackageInstalls()
}

/**
 * Returns true if this package is a split APK (i.e. has multiple split source dirs).
 * Split APKs cannot be used directly as a patch source.
 */
fun PackageInfo.isSplitApk(): Boolean =
    !applicationInfo?.splitSourceDirs.isNullOrEmpty() || splitNames.isNotEmpty()

