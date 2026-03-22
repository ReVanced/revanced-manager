package app.revanced.manager.domain.repository

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.utils.getOrThrow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
sealed interface ChangelogSource : Parcelable {
    data object Manager : ChangelogSource
    data class Patches(val url: String, val prerelease: Boolean) : ChangelogSource {
        val baseUrl by lazy { url.toUri().let { "${it.scheme}://${it.host}" } }
    }
}

class ChangelogsRepository(
    private val api: ReVancedAPI,
    private val source: ChangelogSource,
) : PagingSource<Int, ReVancedAssetHistory>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReVancedAssetHistory> {
        return try {
            val items = when (source) {
                is ChangelogSource.Manager ->
                    api.getAppHistory().getOrThrow()

                is ChangelogSource.Patches ->
                    api.getPatchesHistory(source.baseUrl, source.prerelease).getOrThrow()
            }

            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ReVancedAssetHistory>): Int? = null
}