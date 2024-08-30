@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.example

import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import app.revanced.manager.plugin.downloader.download
import app.revanced.manager.plugin.downloader.downloader
import app.revanced.manager.plugin.downloader.requestStartActivity
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

// TODO: document and update API (update requestUserInteraction, add bound service function), change dispatcher, finish UI

@Parcelize
class InstalledApp(val path: String) : Parcelable

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
        if (version != null && packageInfo.versionName != version) return@get null

        requestStartActivity<InteractionActivity>(pluginPackageName)

        InstalledApp(packageInfo.applicationInfo.sourceDir) to packageInfo.versionName
    }

    download { app ->
        with(Path(app.path)) { inputStream() to fileSize() }
    }

    download { app, outputStream ->
        val path = Path(app.path)
        reportSize(path.fileSize())
        Files.copy(path, outputStream)
    }
}