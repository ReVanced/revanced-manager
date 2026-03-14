package app.revanced.manager.data.room.apps.installed

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_app")
    fun getAll(): Flow<List<InstalledApp>>

    @Query("SELECT * FROM installed_app WHERE current_package_name = :packageName")
    suspend fun get(packageName: String): InstalledApp?

    @Query(
        "SELECT bundle, patch_name FROM applied_patch" +
                " WHERE package_name = :packageName"
    )
    suspend fun getPatchesSelection(packageName: String): Map<@MapColumn("bundle") Int, List<@MapColumn(
        "patch_name"
    ) String>>

    @Query("SELECT * FROM installed_patch_bundle WHERE package_name = :packageName")
    suspend fun getInstalledPatchBundles(packageName: String): List<InstalledPatchBundle>

    @Transaction
    suspend fun upsertApp(
        installedApp: InstalledApp,
        appliedPatches: List<AppliedPatch>,
        patchBundles: List<InstalledPatchBundle>
    ) {
        upsertApp(installedApp)
        deleteAppliedPatches(installedApp.currentPackageName)
        deleteInstalledPatchBundles(installedApp.currentPackageName)
        insertAppliedPatches(appliedPatches)
        insertInstalledPatchBundles(patchBundles)
    }

    @Upsert
    suspend fun upsertApp(installedApp: InstalledApp)

    @Insert
    suspend fun insertAppliedPatches(appliedPatches: List<AppliedPatch>)

    @Insert
    suspend fun insertInstalledPatchBundles(patchBundles: List<InstalledPatchBundle>)

    @Query("DELETE FROM applied_patch WHERE package_name = :packageName")
    suspend fun deleteAppliedPatches(packageName: String)

    @Query("DELETE FROM installed_patch_bundle WHERE package_name = :packageName")
    suspend fun deleteInstalledPatchBundles(packageName: String)

    @Delete
    suspend fun delete(installedApp: InstalledApp)
}