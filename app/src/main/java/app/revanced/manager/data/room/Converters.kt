package app.revanced.manager.data.room

import androidx.room.TypeConverter
import app.revanced.manager.data.room.bundles.Source
import io.ktor.http.*
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
}