package app.revanced.manager.plugin.downloader.example

import android.content.pm.PackageManager
import androidx.paging.PagingConfig
import app.revanced.manager.plugin.downloader.DownloaderPlugin
import app.revanced.manager.plugin.downloader.singlePagePagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

@Suppress("Unused", "MemberVisibilityCanBePrivate")
class DownloaderPluginImpl(downloaderPluginParameters: DownloaderPlugin.Parameters) :
    DownloaderPlugin<DownloaderPluginImpl.AppImpl> {
    private val pm = downloaderPluginParameters.context.packageManager

    private fun getPackageInfo(packageName: String) = try {
        pm.getPackageInfo(packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    override val pagingConfig = PagingConfig(pageSize = 1)

    override fun createPagingSource(parameters: DownloaderPlugin.SearchParameters) =
        singlePagePagingSource {
            val impl = withContext(Dispatchers.IO) { getPackageInfo(parameters.packageName) }?.let {
                AppImpl(
                    parameters.packageName,
                    it.versionName,
                    it.applicationInfo.sourceDir
                )
            }

            listOfNotNull(impl)
        }

    override suspend fun download(
        app: AppImpl, parameters: DownloaderPlugin.DownloadParameters
    ) {
        withContext(Dispatchers.IO) {
            Files.copy(
                Path(app.apkPath),
                parameters.targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    @Parcelize
    class AppImpl(
        override val packageName: String,
        override val version: String,
        internal val apkPath: String
    ) : DownloaderPlugin.App
}