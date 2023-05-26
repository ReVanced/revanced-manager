package app.revanced.manager.compose.domain.repository

import app.revanced.manager.compose.data.room.AppDatabase
import app.revanced.manager.compose.data.room.sources.SourceEntity
import app.revanced.manager.compose.data.room.sources.SourceLocation
import app.revanced.manager.compose.data.room.sources.VersionInfo
import app.revanced.manager.compose.util.apiURL
import kotlin.random.Random
import io.ktor.http.*

class SourcePersistenceRepository(db: AppDatabase) {
    private val dao = db.sourceDao()

    private companion object {
        fun generateUid() = Random.Default.nextInt()

        val defaultSource = SourceEntity(
            uid = generateUid(),
            name = "Official",
            versionInfo = VersionInfo("", ""),
            location = SourceLocation.Remote(Url(apiURL))
        )
    }

    suspend fun loadConfiguration(): List<SourceEntity> {
        val all = dao.all()
        if (all.isEmpty()) {
            dao.add(defaultSource)
            return listOf(defaultSource)
        }

        return all
    }

    suspend fun clear() = dao.purge()

    suspend fun create(name: String, location: SourceLocation): Int {
        val uid = generateUid()
        dao.add(
            SourceEntity(
                uid = uid,
                name = name,
                versionInfo = VersionInfo("", ""),
                location = location,
            )
        )

        return uid
    }

    suspend fun delete(uid: Int) = dao.remove(uid)

    suspend fun updateVersion(uid: Int, patches: String, integrations: String) =
        dao.updateVersion(uid, patches, integrations)

    suspend fun getVersion(id: Int) = dao.getVersionById(id).let { it.patches to it.integrations }
}