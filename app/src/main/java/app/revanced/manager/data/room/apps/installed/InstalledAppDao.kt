package app.revanced.manager.data.room.apps.installed

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_app")
    fun getAll(): Flow<List<InstalledApp>>

    @Query("SELECT * FROM installed_app WHERE current_package_name = :packageName")
    suspend fun get(packageName: String): InstalledApp?

    @MapInfo(keyColumn = "bundle", valueColumn = "patch_name")
    @Query(
        "SELECT bundle, patch_name FROM applied_patch" +
                " WHERE package_name = :packageName"
    )
    suspend fun getPatchesSelection(packageName: String): Map<Int, List<String>>

    @Transaction
    suspend fun insertApp(installedApp: InstalledApp, appliedPatches: List<AppliedPatch>) {
        insertApp(installedApp)
        insertAppliedPatches(appliedPatches)
    }

    @Insert
    suspend fun insertApp(installedApp: InstalledApp)

    @Insert
    suspend fun insertAppliedPatches(appliedPatches: List<AppliedPatch>)

    @Delete
    suspend fun delete(installedApp: InstalledApp)
}