package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Error(val error: String) : ChangelogUiState
    data class Success(
        val changelogs: List<ReVancedAssetHistory>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
    ) : ChangelogUiState
}

@Parcelize
@Serializable
sealed interface ChangelogSource : Parcelable {
    data object Manager : ChangelogSource {
    }
    data class Patches(val url: String) : ChangelogSource {
        val baseUrl get() = url.toUri().let { "${it.scheme}://${it.host}" }
    }
}

class ChangelogsViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
    private val source: ChangelogSource,
) : ViewModel() {

    var state: ChangelogUiState by mutableStateOf(ChangelogUiState.Loading)
        private set

    private var allChangelogs: List<ReVancedAssetHistory> = emptyList()
    private var currentPage = 0
    private val pageSize = 2

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                allChangelogs = when (source) {
                    is ChangelogSource.Manager -> api.getAppHistory().getOrThrow()
                    is ChangelogSource.Patches -> api.getPatchesHistory(source.baseUrl).getOrThrow()
                }

                state = ChangelogUiState.Success(
                    changelogs = allChangelogs.take(pageSize),
                    hasMore = allChangelogs.size > pageSize
                )
                currentPage = 1
            }
            if (state is ChangelogUiState.Loading) {
                state = ChangelogUiState.Error(app.getString(R.string.changelog_download_fail))
            }
        }
    }

    fun loadNextPage() {
        val current = state as? ChangelogUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMore) return

        state = current.copy(isLoadingMore = true)

        val nextItems = allChangelogs.drop(currentPage * pageSize).take(pageSize)
        currentPage++

        state = current.copy(
            changelogs = current.changelogs + nextItems,
            isLoadingMore = false,
            hasMore = currentPage * pageSize < allChangelogs.size
        )
    }
}