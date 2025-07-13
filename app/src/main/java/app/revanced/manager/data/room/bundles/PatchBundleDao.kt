package app.revanced.manager.data.room.bundles

import androidx.room.*

@Dao
interface PatchBundleDao {
    @Query("SELECT * FROM patch_bundles")
    suspend fun all(): List<PatchBundleEntity>

    @Query("UPDATE patch_bundles SET version = :patches WHERE uid = :uid")
    suspend fun updateVersionHash(uid: Int, patches: String?)

    @Query("DELETE FROM patch_bundles WHERE uid != 0")
    suspend fun purgeCustomBundles()

    @Transaction
    suspend fun reset() {
        purgeCustomBundles()
        updateVersionHash(0, null) // Reset the main source
    }

    @Query("DELETE FROM patch_bundles WHERE uid = :uid")
    suspend fun remove(uid: Int)

    @Query("SELECT name, version, auto_update, source FROM patch_bundles WHERE uid = :uid")
    suspend fun getProps(uid: Int): PatchBundleProperties?

    @Upsert
    suspend fun upsert(source: PatchBundleEntity)
}