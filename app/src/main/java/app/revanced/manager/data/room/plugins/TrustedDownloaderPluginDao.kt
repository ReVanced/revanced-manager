package app.revanced.manager.data.room.plugins

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface TrustedDownloaderPluginDao {
    @Query("SELECT signature FROM trusted_downloader_plugins WHERE package_name = :packageName")
    suspend fun getTrustedSignature(packageName: String): ByteArray?

    @Upsert
    suspend fun upsertTrust(plugin: TrustedDownloaderPlugin)

    @Query("DELETE FROM trusted_downloader_plugins WHERE package_name = :packageName")
    suspend fun remove(packageName: String)

    @Transaction
    @Query("DELETE FROM trusted_downloader_plugins WHERE package_name IN (:packageNames)")
    suspend fun removeAll(packageNames: Set<String>)
}