package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.revanced.manager.domain.repository.ChangelogSource
import app.revanced.manager.domain.repository.ChangelogsRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAssetHistory
import kotlinx.coroutines.flow.Flow

class ChangelogsViewModel(
    private val api: ReVancedAPI,
    private val source: ChangelogSource,
) : ViewModel() {
    val changelogs: Flow<PagingData<ReVancedAssetHistory>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { ChangelogsRepository(api, source) }
    ).flow.cachedIn(viewModelScope)
}