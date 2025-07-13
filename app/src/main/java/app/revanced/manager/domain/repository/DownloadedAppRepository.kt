package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.os.Parcelable
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.plugin.downloader.OutputDownloadScope
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FilterOutputStream
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.outputStream

class DownloadedAppRepository(
    private val app: Application,
    db: AppDatabase,
    private val pm: PM
) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    fun getApkFileForApp(app: DownloadedApp): File =
        getApkFileForDir(dir.resolve(app.directory))

    private fun getApkFileForDir(directory: File) = directory.listFiles()!!.first()

    suspend fun download(
        plugin: LoadedDownloaderPlugin,
        data: Parcelable,
        expectedPackageName: String,
        expectedVersion: String?,
        appCompatibilityCheck: Boolean,
        patchesCompatibilityCheck: Boolean,
        onDownload: suspend (downloadProgress: Pair<Long, Long?>) -> Unit,
    ): File {
        // Converted integers cannot contain / or .. unlike the package name or version, so they are safer to use here.
        val relativePath = File(generateUid().toString())
        val saveDir = dir.resolve(relativePath).also { it.mkdirs() }
        val targetFile = saveDir.resolve("base.apk").toPath()

        try {
            val downloadSize = AtomicLong(0)
            val downloadedBytes = AtomicLong(0)

            channelFlow {
                val scope = object : OutputDownloadScope {
                    override val pluginPackageName = plugin.packageName
                    override val hostPackageName = app.packageName
                    override suspend fun reportSize(size: Long) {
                        require(size > 0) { "Size must be greater than zero" }
                        require(
                            downloadSize.compareAndSet(
                                0,
                                size
                            )
                        ) { "Download size has already been set" }
                        send(downloadedBytes.get() to size)
                    }
                }

                fun emitProgress(bytes: Long) {
                    val newValue = downloadedBytes.addAndGet(bytes)
                    val totalSize = downloadSize.get()
                    if (totalSize < 1) return
                    trySend(newValue to totalSize).getOrThrow()
                }

                targetFile.outputStream(StandardOpenOption.CREATE_NEW).buffered().use {
                    val stream = object : FilterOutputStream(it) {
                        override fun write(b: Int) = out.write(b).also { emitProgress(1) }

                        override fun write(b: ByteArray?, off: Int, len: Int) =
                            out.write(b, off, len).also {
                                emitProgress(
                                    (len - off).toLong()
                                )
                            }
                    }
                    plugin.download(scope, data, stream)
                }
            }
                .conflate()
                .flowOn(Dispatchers.IO)
                .collect { (downloaded, size) -> onDownload(downloaded to size) }

            if (downloadedBytes.get() < 1) error("Downloader did not download anything.")
            val pkgInfo =
                pm.getPackageInfo(targetFile.toFile()) ?: error("Downloaded APK file is invalid")
            if (pkgInfo.packageName != expectedPackageName) error("Downloaded APK has the wrong package name. Expected: $expectedPackageName, Actual: ${pkgInfo.packageName}")
            expectedVersion?.let {
                if (
                    pkgInfo.versionName != expectedVersion &&
                    (appCompatibilityCheck || patchesCompatibilityCheck)
                ) error("The selected app version ($pkgInfo.versionName) doesn't match the suggested version. Please use the suggested version ($expectedVersion), or adjust your settings by disabling \"Require suggested app version\" and enabling \"Disable version compatibility check\".")
            }

            // Delete the previous copy (if present).
            dao.get(pkgInfo.packageName, pkgInfo.versionName!!)?.directory?.let {
                if (!dir.resolve(it).deleteRecursively()) throw Exception("Failed to delete existing directory")
            }
            dao.upsert(
                DownloadedApp(
                    packageName = pkgInfo.packageName,
                    version = pkgInfo.versionName!!,
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

    suspend fun get(packageName: String, version: String, markUsed: Boolean = false) =
        dao.get(packageName, version)?.also {
            if (markUsed) dao.markUsed(packageName, version)
        }

    suspend fun delete(downloadedApps: Collection<DownloadedApp>) {
        downloadedApps.forEach {
            dir.resolve(it.directory).deleteRecursively()
        }

        dao.delete(downloadedApps)
    }
}