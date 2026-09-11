package app.revanced.manager.data.room.downloader

import androidx.room.*
import app.revanced.manager.domain.manager.SourceManager
import io.ktor.http.Url

@Entity(tableName = "downloaders")
data class DownloaderEntity(
    @PrimaryKey override val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "url") val url: Url,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean,
    @ColumnInfo(name = "released_at") val releasedAt: Long? = null
) : SourceManager.DatabaseEntity