package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

class DownloadedAppRepository(app: Application, db: AppDatabase) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    private fun getApkFileForApp(app: DownloadedApp): File = getApkFileForDir(dir.resolve(app.directory))
    private fun getApkFileForDir(directory: File) = directory.listFiles()!!.first()

    suspend fun download(
        plugin: LoadedDownloaderPlugin,
        app: App,
        onDownload: suspend (downloadProgress: Pair<Double, Double?>) -> Unit,
    ): File {
        this.get(app.packageName, app.version)?.let { downloaded ->
            return getApkFileForApp(downloaded)
        }

        // Converted integers cannot contain / or .. unlike the package name or version, so they are safer to use here.
        val relativePath = File(generateUid().toString())
        val saveDir = dir.resolve(relativePath).also { it.mkdirs() }
        val targetFile = saveDir.resolve("base.apk")

        try {
            val scope = object : DownloadScope {
                override val targetFile = targetFile
                override suspend fun reportProgress(bytesReceived: Long, bytesTotal: Long?) {
                    require(bytesReceived >= 0) { "bytesReceived must not be negative" }
                    require(bytesTotal == null || bytesTotal >= bytesReceived) { "bytesTotal must be greater than or equal to bytesReceived" }
                    require(bytesTotal != 0L) { "bytesTotal must not be zero" }

                    onDownload(bytesReceived.megaBytes to bytesTotal?.megaBytes)
                }
            }

            plugin.download(scope, app)

            if (!targetFile.exists()) throw Exception("Downloader did not download any files")

            dao.insert(
                DownloadedApp(
                    packageName = app.packageName,
                    version = app.version,
                    directory = relativePath,
                )
            )
        } catch (e: Exception) {
            saveDir.deleteRecursively()
            throw e
        }

        // Return the Apk file.
        return getApkFileForDir(saveDir)
    }

    suspend fun get(packageName: String, version: String) = dao.get(packageName, version)

    suspend fun delete(downloadedApps: Collection<DownloadedApp>) {
        downloadedApps.forEach {
            dir.resolve(it.directory).deleteRecursively()
        }

        dao.delete(downloadedApps)
    }

    private companion object {
        val Long.megaBytes get() = toDouble() / 1_000_000
    }
}