package app.revanced.manager.domain.repository

import android.os.Parcelable
import androidx.core.net.toUri
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.utils.getOrThrow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
sealed interface ChangelogSource : Parcelable {
    data object Manager : ChangelogSource {
    }
    data class Patches(val url: String) : ChangelogSource {
        val baseUrl get() = url.toUri().let { "${it.scheme}://${it.host}" }
    }
}

class ChangelogsRepository(
    private val api: ReVancedAPI
) {
    private var all: List<ReVancedAssetHistory> = emptyList()
    private var page = 0

    suspend fun loadInitial(
        source: ChangelogSource,
        pageSize: Int
    ): PageResult<ReVancedAssetHistory> {
        all = when (source) {
            is ChangelogSource.Manager ->
                api.getAppHistory().getOrThrow()

            is ChangelogSource.Patches ->
                api.getPatchesHistory(source.baseUrl).getOrThrow()
        }

        page = 1

        val items = all.take(pageSize)
        return PageResult(
            items = items,
            hasMore = hasMore(pageSize)
        )
    }

    fun loadNext(pageSize: Int): PageResult<ReVancedAssetHistory> {
        val items = all
            .drop(page * pageSize)
            .take(pageSize)

        page++

        return PageResult(
            items = items,
            hasMore = hasMore(pageSize)
        )
    }

    private fun hasMore(pageSize: Int) =
        page * pageSize < all.size
}

data class PageResult<T>(
    val items: List<T>,
    val hasMore: Boolean
)