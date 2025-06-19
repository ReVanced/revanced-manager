package app.revanced.manager.data.room.bundles

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface PatchBundleDao {
    @Query("SELECT * FROM patch_bundles")
    suspend fun all(): List<PatchBundleEntity>

    @Query("SELECT version, search_update, auto_update FROM patch_bundles WHERE uid = :uid")
    fun getPropsById(uid: Int): Flow<BundleProperties?>

    @Query("SELECT latest_version, latest_changelog, latest_publish_date FROM patch_bundles WHERE uid = :uid")
    fun getLatestPropsById(uid: Int): Flow<RemoteLatestBundleProperties?>

    @Query("SELECT changelog, publish_date FROM patch_bundles WHERE uid = :uid")
    fun getInstalledProps(uid: Int): Flow<RemoteBundleProperties?>

    @Query("UPDATE patch_bundles SET version = :patches WHERE uid = :uid")
    suspend fun updateVersion(uid: Int, patches: String?)

    @Query("UPDATE patch_bundles SET changelog = :changelog, publish_date = :createdAt WHERE uid = :uid")
    suspend fun updateInstallationProps(uid: Int, changelog: String, createdAt: String)

    @Query("UPDATE patch_bundles SET latest_version = :version, latest_changelog = :changelog, latest_publish_date = :createdAt WHERE uid = :uid")
    suspend fun updateLatestRemoteInfo(uid: Int, version: String, changelog: String, createdAt: String)

    @Query("UPDATE patch_bundles SET auto_update = :value WHERE uid = :uid")
    suspend fun setAutoUpdate(uid: Int, value: Boolean)

    @Query("UPDATE patch_bundles SET search_update = :value WHERE uid = :uid")
    suspend fun setSearchUpdate(uid: Int, value: Boolean)

    @Query("UPDATE patch_bundles SET name = :value WHERE uid = :uid")
    suspend fun setName(uid: Int, value: String)

    @Query("DELETE FROM patch_bundles WHERE uid != 0")
    suspend fun purgeCustomBundles()

    @Transaction
    suspend fun reset() {
        purgeCustomBundles()
        updateVersion(0, null) // Reset the main source
    }

    @Query("DELETE FROM patch_bundles WHERE uid = :uid")
    suspend fun remove(uid: Int)

    @Insert
    suspend fun add(source: PatchBundleEntity)
}