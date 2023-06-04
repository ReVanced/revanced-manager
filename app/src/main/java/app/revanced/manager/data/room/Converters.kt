package app.revanced.manager.data.room

import androidx.room.TypeConverter
import app.revanced.manager.data.room.sources.SourceLocation
import io.ktor.http.*

class Converters {
    @TypeConverter
    fun locationFromString(value: String) = when(value) {
        SourceLocation.Local.SENTINEL -> SourceLocation.Local
        else -> SourceLocation.Remote(Url(value))
    }

    @TypeConverter
    fun locationToString(location: SourceLocation) = location.toString()
}