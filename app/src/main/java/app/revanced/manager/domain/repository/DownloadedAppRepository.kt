package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.plugin.downloader.DownloaderPlugin
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

class DownloadedAppRepository(
    app: Application,
    db: AppDatabase,
    private val downloaderPluginRepository: DownloaderPluginRepository
) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    fun getApkFileForApp(app: DownloadedApp): File = getApkFileForDir(dir.resolve(app.directory))
    private fun getApkFileForDir(directory: File) = directory.listFiles()!!.first()

    suspend fun <A : DownloaderPlugin.App> download(
        plugin: DownloaderPlugin<A>,
        app: A,
        onDownload: suspend (downloadProgress: Pair<Float, Float>?) -> Unit,
    ): File {
        this.get(app.packageName, app.version)?.let { downloaded ->
            return getApkFileForApp(downloaded)
        }

        // Converted integers cannot contain / or .. unlike the package name or version, so they are safer to use here.
        val relativePath = File(generateUid().toString())
        val savePath = dir.resolve(relativePath).also { it.mkdirs() }

        try {
            val parameters = DownloaderPlugin.DownloadParameters(
                targetFile = savePath.resolve("base.apk"),
                onDownloadProgress = { progress ->
                    val (bytesReceived, bytesTotal) = progress
                        ?: return@DownloadParameters onDownload(null)

                    onDownload(bytesReceived.megaBytes to bytesTotal.megaBytes)
                }
            )

            plugin.download(app, parameters)

            dao.insert(
                DownloadedApp(
                    packageName = app.packageName,
                    version = app.version,
                    directory = relativePath,
                )
            )
        } catch (e: Exception) {
            savePath.deleteRecursively()
            throw e
        }

        // Return the Apk file.
        return getApkFileForDir(savePath)
    }

    suspend fun get(packageName: String, version: String) = dao.get(packageName, version)

    suspend fun delete(downloadedApps: Collection<DownloadedApp>) {
        downloadedApps.forEach {
            dir.resolve(it.directory).deleteRecursively()
        }

        dao.delete(downloadedApps)
    }

    private companion object {
        val Int.megaBytes get() = div(100000).toFloat() / 10
    }
}