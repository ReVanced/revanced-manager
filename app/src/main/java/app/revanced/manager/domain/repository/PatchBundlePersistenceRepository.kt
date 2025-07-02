package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.bundles.BundleProperties
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.data.room.bundles.Source
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDateTime

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

    suspend fun create(name: String, source: Source, searchUpdate: Boolean = false, autoUpdate: Boolean = false) =
        PatchBundleEntity(
            uid = generateUid(),
            name = name,
            source = source,
            properties = BundleProperties(
                version = null,
                searchUpdate = searchUpdate,
                autoUpdate = autoUpdate
            )
        ).also {
            dao.add(it)
        }

    suspend fun delete(uid: Int) = dao.remove(uid)

    suspend fun updateVersion(uid: Int, version: String?) =
        dao.updateVersion(uid, version)

    suspend fun updateInstallationProps(uid: Int, changelog: String, createdAt: String) =
        dao.updateInstallationProps(uid, changelog, createdAt)

    suspend fun updateLatestRemoteInfo(uid: Int, version: String, changelog: String, createdAt: String) =
        dao.updateLatestRemoteInfo(uid, version, changelog, createdAt)

    suspend fun setAutoUpdate(uid: Int, value: Boolean) = dao.setAutoUpdate(uid, value)

    suspend fun setSearchUpdate(uid: Int, value: Boolean) = dao.setSearchUpdate(uid, value)

    suspend fun setName(uid: Int, name: String) = dao.setName(uid, name)

    fun getProps(id: Int) = dao.getPropsById(id).distinctUntilChanged()

    fun getLatestProps(id: Int) = dao.getLatestPropsById(id).distinctUntilChanged()

    fun getInstalledProps(id: Int) = dao.getInstalledProps(id).distinctUntilChanged()

    private companion object {
        val defaultSource = PatchBundleEntity(
            uid = 0,
            name = "",
            properties = BundleProperties(
                version = null,
                searchUpdate = false,
                autoUpdate = false
            ),
            source = Source.API
        )
    }
}