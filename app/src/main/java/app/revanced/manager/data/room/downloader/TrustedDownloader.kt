package app.revanced.manager.data.room.downloader

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_downloader")
class TrustedDownloader(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "signature") val signature: ByteArray
)