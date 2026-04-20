package app.revanced.manager.data.room.sources

import androidx.room.ColumnInfo
import io.ktor.http.Url


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

data class SourceProperties(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "version") val versionHash: String? = null,
    @ColumnInfo(name = "source") val source: Source,
    @ColumnInfo(name = "auto_update") val autoUpdate: Boolean,
    @ColumnInfo(name = "released_at") val releasedAt: Long? = null,
)