package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

class DownloadedAppRepository(
    db: AppDatabase
) {
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    suspend fun get(packageName: String, version: String) = dao.get(packageName, version)

    suspend fun add(
        packageName: String,
        version: String,
        file: File
    ) = dao.insert(
        DownloadedApp(
            packageName = packageName,
            version = version,
            file = file
        )
    )

    suspend fun delete(downloadedApps: Collection<DownloadedApp>) {
        downloadedApps.forEach {
            it.file.deleteRecursively()
        }

        dao.delete(downloadedApps)
    }
}