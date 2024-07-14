package app.revanced.manager.data.room

import androidx.room.TypeConverter
import app.revanced.manager.data.room.bundles.Source
import app.revanced.manager.data.room.options.Option.SerializedValue
import java.io.File

class Converters {
    @TypeConverter
    fun sourceFromString(value: String) = Source.from(value)

    @TypeConverter
    fun sourceToString(value: Source) = value.toString()

    @TypeConverter
    fun fileFromString(value: String) = File(value)

    @TypeConverter
    fun fileToString(file: File): String = file.path

    @TypeConverter
    fun serializedOptionFromString(value: String) = SerializedValue.fromJsonString(value)

    @TypeConverter
    fun serializedOptionToString(value: SerializedValue) = value.toJsonString()
}