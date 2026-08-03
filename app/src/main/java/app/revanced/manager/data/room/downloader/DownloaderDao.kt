package app.revanced.manager.data.room.downloader

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.data.room.sources.SourceUrl

@Dao
interface DownloaderDao {
    @Query("SELECT * FROM downloaders")
    suspend fun all(): List<DownloaderEntity>

    @Query("UPDATE downloaders SET version = :patches WHERE uid = :uid")
    suspend fun updateVersionHash(uid: Int, patches: String?)

    @Query("DELETE FROM downloaders WHERE uid != 0")
    suspend fun purgeCustomDownloaders()

    @Transaction
    suspend fun reset() {
        purgeCustomDownloaders()
        updateVersionHash(0, null) // Reset the main source
    }

    @Query("DELETE FROM downloaders WHERE uid = :uid")
    suspend fun remove(uid: Int)

    @Query("SELECT name, version, auto_update, url, released_at FROM downloaders WHERE uid = :uid")
    suspend fun getProps(uid: Int): SourceProperties?

    @Query("SELECT uid, url FROM downloaders")
    suspend fun allUrls(): List<SourceUrl>

    @Query("UPDATE downloaders SET url = :url WHERE uid = :uid")
    suspend fun setUrl(uid: Int, url: String)

    @Upsert
    suspend fun upsert(source: DownloaderEntity)
}