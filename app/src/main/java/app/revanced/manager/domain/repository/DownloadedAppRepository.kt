package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.network.downloader.AppDownloader
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

class DownloadedAppRepository(
    app: Application,
    db: AppDatabase
) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    fun getApkFileForApp(app: DownloadedApp): File = getApkFileForDir(dir.resolve(app.directory))
    private fun getApkFileForDir(directory: File) = directory.listFiles()!!.first()

    suspend fun download(
        app: AppDownloader.App,
        preferSplits: Boolean,
        onDownload: suspend (downloadProgress: Pair<Float, Float>?) -> Unit = {},
    ): File {
        this.get(app.packageName, app.version)?.let { downloaded ->
            return getApkFileForApp(downloaded)
        }

        // Converted integers cannot contain / or .. unlike the package name or version, so they are safer to use here.
        val relativePath = File(generateUid().toString())
        val savePath = dir.resolve(relativePath).also { it.mkdirs() }

        try {
            app.download(savePath, preferSplits, onDownload)

            dao.insert(DownloadedApp(
                packageName = app.packageName,
                version = app.version,
                directory = relativePath,
            ))
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
}