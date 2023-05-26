package app.revanced.manager.compose.data.room.sources

import androidx.room.*
import io.ktor.http.*

const val sourcesTableName = "sources"

sealed class SourceLocation {
    object Local : SourceLocation() {
        const val SENTINEL = "local"

        override fun toString() = SENTINEL
    }

    data class Remote(val url: Url) : SourceLocation() {
        override fun toString() = url.toString()
    }
}

data class VersionInfo(
    @ColumnInfo(name = "version") val patches: String,
    @ColumnInfo(name = "integrations_version") val integrations: String,
)

@Entity(tableName = sourcesTableName, indices = [Index(value = ["name"], unique = true)])
data class SourceEntity(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @Embedded val versionInfo: VersionInfo,
    @ColumnInfo(name = "location") val location: SourceLocation,
)