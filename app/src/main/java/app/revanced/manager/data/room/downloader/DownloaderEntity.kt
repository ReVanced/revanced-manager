package app.revanced.manager.data.room.downloader

import androidx.room.*
import app.revanced.manager.data.room.sources.Source
import app.revanced.manager.domain.manager.SourceManager

@Entity(tableName = "downloaders")
data class DownloaderEntity(
    @PrimaryKey override val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
) : SourceManager.DatabaseEntity