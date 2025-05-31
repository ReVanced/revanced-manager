package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.selection.PatchSelection

class PatchSelectionRepository(db: AppDatabase) {
    private val dao = db.selectionDao()

    private suspend fun getOrCreateSelection(bundleUid: Int, packageName: String) =
        dao.getSelectionId(bundleUid, packageName) ?: PatchSelection(
            uid = generateUid(),
            patchBundle = bundleUid,
            packageName = packageName
        ).also { dao.createSelection(it) }.uid

    suspend fun getSelection(packageName: String): Map<Int, Set<String>> =
        dao.getSelectedPatches(packageName).mapValues { it.value.toSet() }

    suspend fun updateSelection(packageName: String, selection: Map<Int, Set<String>>) =
        dao.updateSelections(selection.mapKeys { (sourceUid, _) ->
            getOrCreateSelection(
                sourceUid,
                packageName
            )
        })

    suspend fun clearSelection(packageName: String) {
        dao.clearForPackage(packageName)
    }

    suspend fun reset() = dao.reset()

    suspend fun export(bundleUid: Int): SerializedSelection = dao.exportSelection(bundleUid)

    suspend fun import(bundleUid: Int, selection: SerializedSelection) {
        dao.clearForPatchBundle(bundleUid)
        dao.updateSelections(selection.entries.associate { (packageName, patches) ->
            getOrCreateSelection(bundleUid, packageName) to patches.toSet()
        })
    }
}

/**
 * A [Map] of package name -> selected patches.
 */
typealias SerializedSelection = Map<String, List<String>>