package app.revanced.manager.data.room.bundles

import androidx.room.*
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.data.room.sources.SourceUrl

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

    @Query("SELECT name, version, auto_update, url, released_at FROM patch_bundles WHERE uid = :uid")
    suspend fun getProps(uid: Int): SourceProperties?

    @Query("SELECT uid, url FROM patch_bundles")
    suspend fun allUrls(): List<SourceUrl>

    @Query("UPDATE patch_bundles SET url = :url WHERE uid = :uid")
    suspend fun setUrl(uid: Int, url: String)

    @Upsert
    suspend fun upsert(source: PatchBundleEntity)
}