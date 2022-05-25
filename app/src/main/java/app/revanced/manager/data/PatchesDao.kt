package app.revanced.manager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatch(patch: PatchEntity)

    @Update
    suspend fun update(patch: PatchEntity)

    @Delete
    suspend fun removePatch(patch: PatchEntity)

    @Query("SELECT * FROM PatchEntity WHERE name = :name LIMIT 1")
    suspend fun getPatchByName(name: String): PatchEntity?

    @Query("SELECT enabled FROM PatchEntity WHERE name = :name LIMIT 1")
    suspend fun isPatchEnabled(name: String): Boolean

    @Query("SELECT * FROM PatchEntity")
    fun getAllPatches(): Flow<Array<PatchEntity>>
}