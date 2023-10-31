package app.revanced.manager.data.room.options

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "options",
    primaryKeys = ["group", "patch_name", "key"],
    foreignKeys = [ForeignKey(
        OptionGroup::class,
        parentColumns = ["uid"],
        childColumns = ["group"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Option(
    @ColumnInfo(name = "group") val group: Int,
    @ColumnInfo(name = "patch_name") val patchName: String,
    @ColumnInfo(name = "key") val key: String,
    // Encoded as Json.
    @ColumnInfo(name = "value") val value: String,
)