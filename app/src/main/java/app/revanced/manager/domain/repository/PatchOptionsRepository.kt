package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.options.Option
import app.revanced.manager.data.room.options.OptionGroup
import app.revanced.manager.util.Options
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

class PatchOptionsRepository(db: AppDatabase) {
    private val dao = db.optionDao()

    private suspend fun getOrCreateGroup(bundleUid: Int, packageName: String) =
        dao.getGroupId(bundleUid, packageName) ?: OptionGroup(
            uid = AppDatabase.generateUid(),
            patchBundle = bundleUid,
            packageName = packageName
        ).also { dao.createOptionGroup(it) }.uid

    suspend fun getOptions(packageName: String): Options {
        val options = dao.getOptions(packageName)
        // Bundle -> Patches
        return buildMap<Int, MutableMap<String, MutableMap<String, Any?>>>(options.size) {
            options.forEach { (sourceUid, bundleOptions) ->
                // Patches -> Patch options
                this[sourceUid] = bundleOptions.fold(mutableMapOf()) { map, option ->
                    val optionsForPatch = map.getOrPut(option.patchName, ::mutableMapOf)

                    optionsForPatch[option.key] = deserialize(option.value)

                    map
                }
            }
        }
    }

    suspend fun saveOptions(packageName: String, options: Options) =
        dao.updateOptions(options.entries.associate { (sourceUid, bundleOptions) ->
            val groupId = getOrCreateGroup(sourceUid, packageName)

            groupId to bundleOptions.flatMap { (patchName, patchOptions) ->
                patchOptions.mapNotNull { (key, value) ->
                    val serialized = serialize(value)
                        ?: return@mapNotNull null // Don't save options that we can't serialize.

                    Option(groupId, patchName, key, serialized)
                }
            }
        })

    fun getPackagesWithSavedOptions() = dao.getPackagesWithOptions().map { it.toSet() }

    suspend fun clearOptionsForPackage(packageName: String) = dao.clearForPackage(packageName)
    suspend fun clearOptionsForPatchBundle(uid: Int) = dao.clearForPatchBundle(uid)
    suspend fun reset() = dao.reset()

    private companion object {
        fun deserialize(value: String): Any? {
            val primitive = Json.decodeFromString<JsonPrimitive>(value)

            return when {
                primitive.isString -> primitive.content
                primitive is JsonNull -> null
                else -> primitive.booleanOrNull ?: primitive.intOrNull ?: primitive.floatOrNull
            }
        }

        fun serialize(value: Any?): String? {
            val primitive = when (value) {
                null -> JsonNull
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> return null
            }

            return Json.encodeToString(primitive)
        }
    }
}