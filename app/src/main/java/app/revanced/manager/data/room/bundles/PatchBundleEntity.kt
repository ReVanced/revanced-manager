package app.revanced.manager.data.room.bundles

import androidx.room.*
import io.ktor.http.*

sealed class Source {
    object Local : Source() {
        const val SENTINEL = "local"

        override fun toString() = SENTINEL
    }

    object API : Source() {
        const val SENTINEL = "api"

        override fun toString() = SENTINEL
    }

    data class Remote(val url: Url) : Source() {
        override fun toString() = url.toString()
    }

    companion object {
        fun from(value: String) = when(value) {
            Local.SENTINEL -> Local
            API.SENTINEL -> API
            else -> Remote(Url(value))
        }
    }
}

data class VersionInfo(
    @ColumnInfo(name = "version") val patches: String? = null,
    @ColumnInfo(name = "integrations_version") val integrations: String? = null,
)

@Entity(tableName = "patch_bundles", indices = [Index(value = ["name"], unique = true)])
data class PatchBundleEntity(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @Embedded val versionInfo: VersionInfo,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)

data class BundleProperties(
    @Embedded val versionInfo: VersionInfo,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)