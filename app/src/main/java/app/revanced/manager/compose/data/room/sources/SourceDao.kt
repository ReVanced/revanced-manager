package app.revanced.manager.compose.data.room.sources

import androidx.room.*

@Dao
interface SourceDao {
    @Query("SELECT * FROM $sourcesTableName")
    suspend fun all(): List<SourceEntity>

    @Query("SELECT version, integrations_version FROM $sourcesTableName WHERE uid = :uid")
    suspend fun getVersionById(uid: Int): VersionInfo

    @Query("UPDATE $sourcesTableName SET version=:patches, integrations_version=:integrations WHERE uid=:uid")
    suspend fun updateVersion(uid: Int, patches: String, integrations: String)

    @Query("DELETE FROM $sourcesTableName")
    suspend fun purge()

    @Query("DELETE FROM $sourcesTableName WHERE uid=:uid")
    suspend fun remove(uid: Int)

    @Insert
    suspend fun add(source: SourceEntity)
}