package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.repository.ChangelogSource
import app.revanced.manager.domain.repository.ChangelogsRepository
import app.revanced.manager.ui.component.ChangelogUiState
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class ChangelogsViewModel(
    private val repository: ChangelogsRepository,
    private val app: Application,
    private val source: ChangelogSource,
) : ViewModel() {

    var state: ChangelogUiState by mutableStateOf(ChangelogUiState.Loading)
        private set

    private val pageSize = 2

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                val result = repository.loadInitial(source, pageSize)

                state = ChangelogUiState.Success(
                    changelogs = result.items,
                    hasMore = result.hasMore
                )
            }

            if (state is ChangelogUiState.Loading) {
                state = ChangelogUiState.Error(
                    app.getString(R.string.changelog_download_fail)
                )
            }
        }
    }

    fun loadNextPage() {
        val current = state as? ChangelogUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMore) return

        state = current.copy(isLoadingMore = true)

        val result = repository.loadNext(pageSize)

        state = current.copy(
            changelogs = current.changelogs + result.items,
            isLoadingMore = false,
            hasMore = result.hasMore
        )
    }
}