package app.revanced.manager.data.room.apps.downloaded

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedAppDao {
    @Query("SELECT * FROM downloaded_app")
    fun getAllApps(): Flow<List<DownloadedApp>>

    @Query("SELECT * FROM downloaded_app WHERE package_name = :packageName AND version = :version")
    suspend fun get(packageName: String, version: String): DownloadedApp?

    @Upsert
    suspend fun upsert(downloadedApp: DownloadedApp)

    @Query("UPDATE downloaded_app SET last_used = :newValue WHERE package_name = :packageName AND version = :version")
    suspend fun markUsed(packageName: String, version: String, newValue: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(downloadedApps: Collection<DownloadedApp>)
}