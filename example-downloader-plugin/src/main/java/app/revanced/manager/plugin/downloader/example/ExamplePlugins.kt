@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.example

import android.content.Intent
import android.content.pm.PackageManager
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloaderContext
import app.revanced.manager.plugin.downloader.downloader
import app.revanced.manager.plugin.downloader.example.BuildConfig.PLUGIN_PACKAGE_NAME
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

// TODO: document API, change dispatcher.

@Parcelize
class InstalledApp(
    override val packageName: String,
    override val version: String,
    internal val apkPath: String
) : App(packageName, version)

fun installedAppDownloader(context: DownloaderContext) = downloader<InstalledApp> {
    val pm = context.androidContext.packageManager

    get { packageName, version ->
        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return@get null
        }

        requestUserInteraction().launch(Intent().apply {
            setClassName(
                PLUGIN_PACKAGE_NAME,
                InteractionActivity::class.java.canonicalName!!
            )
        })

        InstalledApp(
            packageName,
            packageInfo.versionName,
            packageInfo.applicationInfo.sourceDir
        ).takeIf { version == null || it.version == version }
    }

    download {
        // Simulate download progress
        for (i in 0..5) {
            reportProgress(i.megaBytes, 5.megaBytes)
            delay(1000L)
        }

        Files.copy(Path(it.apkPath), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private val Int.megaBytes get() = times(1_000_000)