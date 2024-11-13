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
    @ColumnInfo(name = "version") val version: String? = null,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)

data class BundleProperties(
    @ColumnInfo(name = "version") val version: String? = null,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean
)