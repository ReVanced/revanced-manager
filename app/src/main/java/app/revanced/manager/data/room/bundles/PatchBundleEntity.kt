package app.revanced.manager.data.room.bundles

import androidx.room.*
import app.revanced.manager.data.room.sources.Source

@Entity(tableName = "patch_bundles")
data class PatchBundleEntity(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)