@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.example

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.download
import app.revanced.manager.plugin.downloader.downloader
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

// TODO: document and update API, change dispatcher, finish UI

@Parcelize
class InstalledApp(
    override val packageName: String,
    override val version: String,
    internal val apkPath: String
) : App(packageName, version)

private val application by lazy {
    // Don't do this in a real plugin.
    val clazz = Class.forName("android.app.ActivityThread")
    val activityThread = clazz.getMethod("currentActivityThread")(null)
    clazz.getMethod("getApplication")(activityThread) as Application
}

val installedAppDownloader = downloader<InstalledApp> {
    val pm = application.packageManager

    get { packageName, version ->
        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return@get null
        }

        requestUserInteraction().launch(Intent().apply {
            setClassName(
                pluginPackageName,
                InteractionActivity::class.java.canonicalName!!
            )
        })

        InstalledApp(
            packageName,
            packageInfo.versionName,
            packageInfo.applicationInfo.sourceDir
        ).takeIf { version == null || it.version == version }
    }

    download { app ->
        with(Path(app.apkPath)) { inputStream() to fileSize() }
    }

    download { app, outputStream ->
        val path = Path(app.apkPath)
        reportSize(path.fileSize())
        Files.copy(path, outputStream)
    }
}