package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PatchSelectionRepository(db: AppDatabase) {
    private val dao = db.selectionDao()

    suspend fun getSelection(packageName: String): Map<Int, Set<String>> =
        dao.getSelectedPatches(packageName).mapValues { it.value.toSet() }

    suspend fun updateSelection(packageName: String, selection: Map<Int, Set<String>>) =
        dao.updateSelections(selection.mapKeys { (sourceUid, _) ->
            dao.getOrCreateSelectionId(
                sourceUid,
                packageName
            )
        })

    fun getPackagesWithSavedSelection() =
        dao.getPackagesWithSelection().map(Iterable<String>::toSet).distinctUntilChanged()

    fun getSelectionPackageCount() = dao.getSelectionPackageCount().distinctUntilChanged()

    fun getSelectedPatchCount() = dao.getSelectedPatchCount().distinctUntilChanged()

    suspend fun resetSelectionForPackage(packageName: String) {
        dao.resetForPackage(packageName)
    }

    suspend fun resetSelectionForPatchBundle(uid: Int) {
        dao.resetForPatchBundle(uid)
    }

    suspend fun reset() = dao.reset()

    suspend fun export(bundleUid: Int): SerializedSelection = dao.exportSelection(bundleUid)

    suspend fun import(bundleUid: Int, selection: SerializedSelection) {
        dao.replaceForPatchBundle(
            bundleUid,
            selection.mapValues { (_, patches) -> patches.toSet() }
        )
    }
}

/**
 * A [Map] of package name -> selected patches.
 */
typealias SerializedSelection = Map<String, List<String>>