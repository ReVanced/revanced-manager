package app.revanced.manager.data.room.apps

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM downloaded_app")
    fun getAllApps(): Flow<List<DownloadedApp>>

    @Query("SELECT * FROM downloaded_app WHERE package_name = :packageName AND version = :version")
    suspend fun get(packageName: String, version: String): DownloadedApp?

    @Insert
    suspend fun insert(downloadedApp: DownloadedApp)

    @Delete
    suspend fun delete(downloadedApps: Collection<DownloadedApp>)
}