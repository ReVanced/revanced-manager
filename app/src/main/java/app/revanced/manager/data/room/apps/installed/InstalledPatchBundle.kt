package app.revanced.manager.data.room.apps.installed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "installed_patch_bundle",
    primaryKeys = ["package_name", "bundle_uid"],
    foreignKeys = [
        ForeignKey(
            InstalledApp::class,
            parentColumns = ["current_package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InstalledPatchBundle(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "bundle_uid") val bundleUid: Int,
    @ColumnInfo(name = "bundle_name") val bundleName: String,
    @ColumnInfo(name = "bundle_version") val bundleVersion: String?
)
