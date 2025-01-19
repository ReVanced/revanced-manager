@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.example

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Parcelable
import app.revanced.manager.plugin.downloader.Downloader
import app.revanced.manager.plugin.downloader.requestStartActivity
import app.revanced.manager.plugin.downloader.webview.WebViewDownloader
import kotlinx.parcelize.Parcelize
import kotlin.io.path.*

val apkMirrorDownloader = WebViewDownloader { packageName, version ->
    with(Uri.Builder()) {
        scheme("https")
        authority("www.apkmirror.com")
        mapOf(
            "post_type" to "app_release",
            "searchtype" to "apk",
            "s" to (version?.let { "$packageName $it" } ?: packageName),
            "bundles%5B%5D" to "apk_files" // bundles[]
        ).forEach { (key, value) ->
            appendQueryParameter(key, value)
        }

        build().toString()
    }
}

@Parcelize
class InstalledApp(val path: String) : Parcelable

private val application by lazy {
    // Don't do this in a real plugin.
    val clazz = Class.forName("android.app.ActivityThread")
    val activityThread = clazz.getMethod("currentActivityThread")(null)
    clazz.getMethod("getApplication")(activityThread) as Application
}

val installedAppDownloader = Downloader<InstalledApp> {
    val pm = application.packageManager

    get { packageName, version ->
        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return@get null
        }
        if (version != null && packageInfo.versionName != version) return@get null

        requestStartActivity<InteractionActivity>()

        InstalledApp(packageInfo.applicationInfo!!.sourceDir) to packageInfo.versionName
    }


    download { app ->
        with(Path(app.path)) { inputStream() to fileSize() }
    }

    /*
    download { app, outputStream ->
        val path = Path(app.path)
        reportSize(path.fileSize())
        Files.copy(path, outputStream)
    }*/
}
