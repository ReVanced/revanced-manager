package app.revanced.manager.data.room.apps.downloaded

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.File

@Entity(
    tableName = "downloaded_app",
    primaryKeys = ["package_name", "version"]
)
data class DownloadedApp(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "directory") val directory: File,
    @ColumnInfo(name = "last_used") val lastUsed: Long = System.currentTimeMillis()
)