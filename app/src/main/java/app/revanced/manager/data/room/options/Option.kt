package app.revanced.manager.data.room.options

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.revanced.manager.patcher.patch.Option
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Entity(
    tableName = "options",
    primaryKeys = ["group", "patch_name", "key"],
    foreignKeys = [ForeignKey(
        OptionGroup::class,
        parentColumns = ["uid"],
        childColumns = ["group"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Option(
    @ColumnInfo(name = "group") val group: Int,
    @ColumnInfo(name = "patch_name") val patchName: String,
    @ColumnInfo(name = "key") val key: String,
    // Encoded as Json.
    @ColumnInfo(name = "value") val value: SerializedValue,
) {
    @Serializable
    data class SerializedValue(val raw: JsonElement) {
        fun toJsonString() = json.encodeToString(raw)
        fun deserializeFor(option: Option<*>): Any? {
            if (raw is JsonNull) return null

            val errorMessage = "Cannot deserialize value as ${option.type}"
            try {
                if (option.type.classifier == List::class) {
                    val elementType = option.type.arguments.first().type!!
                    return raw.jsonArray.map { deserializeBasicType(elementType, it.jsonPrimitive) }
                }

                return deserializeBasicType(option.type, raw.jsonPrimitive)
            } catch (e: IllegalArgumentException) {
                throw SerializationException(errorMessage, e)
            } catch (e: IllegalStateException) {
                throw SerializationException(errorMessage, e)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw SerializationException(errorMessage, e)
            }
        }

        companion object {
            private val json = Json {
                // Patcher does not forbid the use of these values, so we should support them.
                allowSpecialFloatingPointValues = true
            }

            private fun deserializeBasicType(type: KType, value: JsonPrimitive) = when (type) {
                typeOf<Boolean>() -> value.boolean
                typeOf<Int>() -> value.int
                typeOf<Long>() -> value.long
                typeOf<Float>() -> value.float
                typeOf<String>() -> value.content.also {
                    if (!value.isString) throw SerializationException(
                        "Expected value to be a string: $value"
                    )
                }

                else -> throw SerializationException("Unknown type: $type")
            }

            fun fromJsonString(value: String) = SerializedValue(json.decodeFromString(value))
            fun fromValue(value: Any?) = SerializedValue(when (value) {
                null -> JsonNull
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is List<*> -> buildJsonArray {
                    var elementClass: KClass<out Any>? = null

                    value.forEach {
                        when (it) {
                            null -> throw SerializationException("List elements must not be null")
                            is Number -> add(it)
                            is Boolean -> add(it)
                            is String -> add(it)
                            else -> throw SerializationException("Unknown element type: ${it::class.simpleName}")
                        }

                        if (elementClass == null) elementClass = it::class
                        else if (elementClass != it::class) throw SerializationException("List elements must have the same type")
                    }
                }

                else -> throw SerializationException("Unknown type: ${value::class.simpleName}")
            })
        }
    }

    class SerializationException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
}
