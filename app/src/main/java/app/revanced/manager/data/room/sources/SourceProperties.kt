package app.revanced.manager.data.room.sources

import androidx.room.ColumnInfo
import io.ktor.http.Url

// The URL of a source as it is stored, used to migrate rows written before sources were URLs.
data class SourceUrl(
    @ColumnInfo(name = "uid") val uid: Int,
    @ColumnInfo(name = "url") val url: String,
)

// The columns of a source row that can be updated after it was created.
data class SourceProperties(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "url") val url: Url,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean,
    @ColumnInfo(name = "released_at") val releasedAt: Long? = null,
)
