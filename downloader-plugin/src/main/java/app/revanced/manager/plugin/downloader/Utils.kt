package app.revanced.manager.plugin.downloader

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException

/**
 * Creates a [PagingSource] that loads one page containing the return value of [block].
 */
fun <A : DownloaderPlugin.App> singlePagePagingSource(block: suspend () -> List<A>): PagingSource<Nothing, A> =
    object : PagingSource<Nothing, A>() {
        override fun getRefreshKey(state: PagingState<Nothing, A>) = null

        override suspend fun load(params: LoadParams<Nothing>) = try {
            LoadResult.Page(
                block(),
                nextKey = null,
                prevKey = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }