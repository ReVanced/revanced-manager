package app.revanced.manager.data.room.bundles

import androidx.room.*
import app.revanced.manager.data.room.sources.Source
import app.revanced.manager.domain.manager.SourceManager

@Entity(tableName = "patch_bundles")
data class PatchBundleEntity(
    @PrimaryKey override val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean,
    @ColumnInfo(name = "released_at") val releasedAt: Long? = null,
) : SourceManager.DatabaseEntity