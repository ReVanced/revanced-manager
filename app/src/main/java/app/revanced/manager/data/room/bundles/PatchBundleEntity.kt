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
        fun from(value: String) = when (value) {
            Local.SENTINEL -> Local
            API.SENTINEL -> API
            else -> Remote(Url(value))
        }
    }
}

@Entity(tableName = "patch_bundles")
data class PatchBundleEntity(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "source") val source: Source,
    @Embedded val properties: BundleProperties,
    @Embedded val remoteProperties: RemoteBundleProperties? = null,
    @Embedded val remoteLatestProperties: RemoteLatestBundleProperties? = null
)

data class BundleProperties(
    @ColumnInfo(name = "version") val version: String? = null,
    @ColumnInfo(name = "search_update") val searchUpdate: Boolean,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)

data class RemoteBundleProperties(
    @ColumnInfo(name = "changelog") val changelog: String? = null,
    @ColumnInfo(name = "publish_date") val publishDate: String? = null
)

data class RemoteLatestBundleProperties(
    @ColumnInfo(name = "latest_version") val latestVersion: String? = null,
    @ColumnInfo(name = "latest_changelog") val latestChangelog: String? = null,
    @ColumnInfo(name = "latest_publish_date") val latestPublishDate: String? = null
)