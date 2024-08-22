package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope
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

class DownloadedAppRepository(app: Application, db: AppDatabase) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    private fun getApkFileForApp(app: DownloadedApp): File =
        getApkFileForDir(dir.resolve(app.directory))

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
        val targetFile = saveDir.resolve("base.apk").toPath()

        try {
            val downloadSize = AtomicLong(0)
            val downloadedBytes = AtomicLong(0)

            channelFlow {
                val scope = object : DownloadScope {
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
                    plugin.download(scope, app, stream)
                }
            }
                .conflate()
                .flowOn(Dispatchers.IO)
                .collect { (downloaded, size) -> onDownload(downloaded.megaBytes to size.megaBytes) }

            if (downloadedBytes.get() < 1) throw Exception("Downloader did not download any files")

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