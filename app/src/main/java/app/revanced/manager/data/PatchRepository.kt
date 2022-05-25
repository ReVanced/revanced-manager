package app.revanced.manager.data

import kotlinx.coroutines.flow.Flow

class PatchRepository(
    private val patchesDao: PatchesDao
) {
    suspend fun insertPatch(patch: PatchEntity) {
        patchesDao.insertPatch(patch)
    }

    suspend fun removePatch(patch: PatchEntity) {
        patchesDao.removePatch(patch)
    }

    suspend fun getPatchByName(name: String): PatchEntity? {
        return patchesDao.getPatchByName(name)
    }

    suspend fun isPatchEnabled(name: String): Boolean {
        return patchesDao.isPatchEnabled(name)
    }

    fun getAllPatches(): Flow<Array<PatchEntity>> {
        return patchesDao.getAllPatches()
    }
}