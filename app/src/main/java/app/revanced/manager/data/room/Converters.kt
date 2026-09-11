package app.revanced.manager.data.room

import androidx.room.TypeConverter
import app.revanced.manager.data.room.options.Option.SerializedValue
import io.ktor.http.Url
import java.io.File

class Converters {
    @TypeConverter
    fun urlFromString(value: String) = Url(value)

    @TypeConverter
    fun urlToString(value: Url) = value.toString()

    @TypeConverter
    fun fileFromString(value: String) = File(value)

    @TypeConverter
    fun fileToString(file: File): String = file.path

    @TypeConverter
    fun serializedOptionFromString(value: String) = SerializedValue.fromJsonString(value)

    @TypeConverter
    fun serializedOptionToString(value: SerializedValue) = value.toJsonString()
}