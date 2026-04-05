package app.revanced.manager.data.room.selection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.revanced.manager.data.room.AppDatabase
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SelectionDao {
    @Transaction
    @Query(
        "SELECT patch_bundle, patch_name FROM patch_selections" +
                " LEFT JOIN selected_patches ON uid = selected_patches.selection" +
                " WHERE package_name = :packageName"
    )
    abstract suspend fun getSelectedPatches(packageName: String): Map<@MapColumn("patch_bundle") Int, List<@MapColumn(
        "patch_name"
    ) String>>

    @Transaction
    @Query(
        "SELECT package_name, patch_name FROM patch_selections" +
                " LEFT JOIN selected_patches ON uid = selected_patches.selection" +
                " WHERE patch_bundle = :bundleUid"
    )
    abstract suspend fun exportSelection(bundleUid: Int): Map<@MapColumn("package_name") String, List<@MapColumn(
        "patch_name"
    ) String>>

    @Query("SELECT uid FROM patch_selections WHERE patch_bundle = :bundleUid AND package_name = :packageName")
    abstract suspend fun getSelectionId(bundleUid: Int, packageName: String): Int?

    @Insert
    protected abstract suspend fun createSelectionIfMissing(selection: PatchSelection)

    @Query("SELECT package_name FROM patch_selections")
    abstract fun getPackagesWithSelection(): Flow<List<String>>

    @Query("SELECT COUNT(DISTINCT package_name) FROM patch_selections")
    abstract fun getSelectionPackageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM selected_patches")
    abstract fun getSelectedPatchCount(): Flow<Int>

    @Query("DELETE FROM patch_selections WHERE patch_bundle = :uid")
    abstract suspend fun resetForPatchBundle(uid: Int)

    @Query("DELETE FROM patch_selections WHERE package_name = :packageName")
    abstract suspend fun resetForPackage(packageName: String)

    @Query("DELETE FROM patch_selections")
    abstract suspend fun reset()

    @Insert
    protected abstract suspend fun selectPatches(patches: List<SelectedPatch>)

    @Query("DELETE FROM selected_patches WHERE selection = :selectionId")
    protected abstract suspend fun clearSelection(selectionId: Int)

    @Transaction
    open suspend fun updateSelections(selections: Map<Int, Set<String>>) =
        selections.forEach { (selectionUid, patches) ->
            clearSelection(selectionUid)
            selectPatches(patches.map { SelectedPatch(selectionUid, it) })
        }

    @Transaction
    open suspend fun getOrCreateSelectionId(bundleUid: Int, packageName: String): Int {
        getSelectionId(bundleUid, packageName)?.let { return it }
        createSelectionIfMissing(
            PatchSelection(
                uid = AppDatabase.generateUid(),
                patchBundle = bundleUid,
                packageName = packageName
            )
        )
        return getSelectionId(bundleUid, packageName)
            ?: throw IllegalStateException("Failed to create selection for $packageName")
    }

    @Transaction
    open suspend fun replaceForPatchBundle(bundleUid: Int, selections: Map<String, Set<String>>) {
        resetForPatchBundle(bundleUid)
        selections.forEach { (packageName, patches) ->
            val selectionUid = getOrCreateSelectionId(bundleUid, packageName)
            clearSelection(selectionUid)
            selectPatches(patches.map { SelectedPatch(selectionUid, it) })
        }
    }
}