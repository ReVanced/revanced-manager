package app.revanced.manager.data.room.plugins

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_downloader_plugins")
class TrustedDownloaderPlugin(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "signature") val signature: ByteArray
)