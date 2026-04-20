package app.revanced.manager.data.room.selection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "selected_patches",
    primaryKeys = ["selection", "patch_name"],
    foreignKeys = [ForeignKey(
        PatchSelection::class,
        parentColumns = ["uid"],
        childColumns = ["selection"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SelectedPatch(
    @ColumnInfo(name = "selection") val selection: Int,
    @ColumnInfo(name = "patch_name") val patchName: String
)