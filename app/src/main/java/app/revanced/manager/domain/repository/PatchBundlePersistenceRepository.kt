package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.data.room.bundles.Source
import kotlinx.coroutines.flow.distinctUntilChanged

class PatchBundlePersistenceRepository(db: AppDatabase) {
    private val dao = db.patchBundleDao()

    suspend fun loadConfiguration(): List<PatchBundleEntity> {
        val all = dao.all()
        if (all.isEmpty()) {
            dao.add(defaultSource)
            return listOf(defaultSource)
        }

        return all
    }

    suspend fun reset() = dao.reset()

    suspend fun create(name: String, source: Source, autoUpdate: Boolean = false) =
        PatchBundleEntity(
            uid = generateUid(),
            name = name,
            version = null,
            source = source,
            autoUpdate = autoUpdate
        ).also {
            dao.add(it)
        }

    suspend fun delete(uid: Int) = dao.remove(uid)

    suspend fun updateVersion(uid: Int, version: String?) =
        dao.updateVersion(uid, version)

    suspend fun setAutoUpdate(uid: Int, value: Boolean) = dao.setAutoUpdate(uid, value)

    suspend fun setName(uid: Int, name: String) = dao.setName(uid, name)

    fun getProps(id: Int) = dao.getPropsById(id).distinctUntilChanged()

    private companion object {
        val defaultSource = PatchBundleEntity(
            uid = 0,
            name = "",
            version = null,
            source = Source.API,
            autoUpdate = false
        )
    }
}