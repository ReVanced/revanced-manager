package app.revanced.manager.data.room

import android.net.Uri
import androidx.room.TypeConverter
import app.revanced.manager.data.room.options.Option.SerializedValue
import java.io.File

class Converters {
    @TypeConverter
    fun uriFromString(value: String): Uri = Uri.parse(value)

    @TypeConverter
    fun uriToString(value: Uri) = value.toString()

    @TypeConverter
    fun fileFromString(value: String) = File(value)

    @TypeConverter
    fun fileToString(file: File): String = file.path

    @TypeConverter
    fun serializedOptionFromString(value: String) = SerializedValue.fromJsonString(value)

    @TypeConverter
    fun serializedOptionToString(value: SerializedValue) = value.toJsonString()
}