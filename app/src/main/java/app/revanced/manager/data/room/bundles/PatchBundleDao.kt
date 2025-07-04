package app.revanced.manager.data.room.bundles

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchBundleDao {
    @Query("SELECT * FROM patch_bundles")
    suspend fun all(): List<PatchBundleEntity>

    @Query("SELECT version, auto_update FROM patch_bundles WHERE uid = :uid")
    fun getPropsById(uid: Int): Flow<BundleProperties?>

    @Query("UPDATE patch_bundles SET version = :patches WHERE uid = :uid")
    suspend fun updateVersionHash(uid: Int, patches: String?)

    @Query("UPDATE patch_bundles SET auto_update = :value WHERE uid = :uid")
    suspend fun setAutoUpdate(uid: Int, value: Boolean)

    @Query("UPDATE patch_bundles SET name = :value WHERE uid = :uid")
    suspend fun setName(uid: Int, value: String)

    @Query("DELETE FROM patch_bundles WHERE uid != 0")
    suspend fun purgeCustomBundles()

    @Transaction
    suspend fun reset() {
        purgeCustomBundles()
        updateVersionHash(0, null) // Reset the main source
    }

    @Query("DELETE FROM patch_bundles WHERE uid = :uid")
    suspend fun remove(uid: Int)

    @Insert
    suspend fun add(source: PatchBundleEntity)
}