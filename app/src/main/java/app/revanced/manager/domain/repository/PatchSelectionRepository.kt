package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.selection.PatchSelection
import app.revanced.manager.domain.sources.Source

class PatchSelectionRepository(db: AppDatabase) {
    private val dao = db.selectionDao()

    private suspend fun getOrCreateSelection(sourceUid: Int, packageName: String) =
        dao.getSelectionId(sourceUid, packageName) ?: PatchSelection(
            uid = generateUid(),
            source = sourceUid,
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

    suspend fun reset() = dao.reset()

    suspend fun export(source: Source): SerializedSelection = dao.exportSelection(source.uid)

    suspend fun import(source: Source, selection: SerializedSelection) {
        dao.clearForSource(source.uid)
        dao.updateSelections(selection.entries.associate { (packageName, patches) ->
            getOrCreateSelection(source.uid, packageName) to patches.toSet()
        })
    }
}

/**
 * A [Map] of package name -> selected patches.
 */
typealias SerializedSelection = Map<String, List<String>>