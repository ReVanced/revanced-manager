package app.revanced.manager.data.room.downloader

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.revanced.manager.data.room.sources.SourceProperties

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

    @Query("SELECT name, version, auto_update, source, released_at FROM downloaders WHERE uid = :uid")
    suspend fun getProps(uid: Int): SourceProperties?

    @Upsert
    suspend fun upsert(source: DownloaderEntity)
}