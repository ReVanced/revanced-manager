package app.revanced.manager.data.room.downloader

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface TrustedDownloaderDao {
    @Query("SELECT signature FROM trusted_downloaders WHERE package_name = :packageName")
    suspend fun getTrustedSignature(packageName: String): ByteArray?

    @Upsert
    suspend fun upsertTrust(downloader: TrustedDownloader)

    @Query("DELETE FROM trusted_downloaders WHERE package_name = :packageName")
    suspend fun remove(packageName: String)

    @Transaction
    @Query("DELETE FROM trusted_downloaders WHERE package_name IN (:packageNames)")
    suspend fun removeAll(packageNames: Set<String>)
}