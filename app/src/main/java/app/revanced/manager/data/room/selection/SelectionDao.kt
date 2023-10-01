package app.revanced.manager.data.room.selection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class SelectionDao {
    @Transaction
    @MapInfo(keyColumn = "patch_bundle", valueColumn = "patch_name")
    @Query(
        "SELECT patch_bundle, patch_name FROM patch_selections" +
                " LEFT JOIN selected_patches ON uid = selected_patches.selection" +
                " WHERE package_name = :packageName"
    )
    abstract suspend fun getSelectedPatches(packageName: String): Map<Int, List<String>>

    @Transaction
    @MapInfo(keyColumn = "package_name", valueColumn = "patch_name")
    @Query(
        "SELECT package_name, patch_name FROM patch_selections" +
                " LEFT JOIN selected_patches ON uid = selected_patches.selection" +
                " WHERE patch_bundle = :bundleUid"
    )
    abstract suspend fun exportSelection(bundleUid: Int): Map<String, List<String>>

    @Query("SELECT uid FROM patch_selections WHERE patch_bundle = :bundleUid AND package_name = :packageName")
    abstract suspend fun getSelectionId(bundleUid: Int, packageName: String): Int?

    @Insert
    abstract suspend fun createSelection(selection: PatchSelection)

    @Query("DELETE FROM patch_selections WHERE patch_bundle = :uid")
    abstract suspend fun clearForPatchBundle(uid: Int)

    @Query("DELETE FROM patch_selections WHERE package_name = :packageName")
    abstract suspend fun clearForPackage(packageName: String)

    @Query("DELETE FROM patch_selections")
    abstract suspend fun reset()

    @Insert
    protected abstract suspend fun selectPatches(patches: List<SelectedPatch>)

    @Query("DELETE FROM selected_patches WHERE selection = :selectionId")
    protected abstract suspend fun clearSelection(selectionId: Int)

    @Transaction
    open suspend fun updateSelections(selections: Map<Int, Set<String>>) =
        selections.map { (selectionUid, patches) ->
            clearSelection(selectionUid)
            selectPatches(patches.map { SelectedPatch(selectionUid, it) })
        }
}