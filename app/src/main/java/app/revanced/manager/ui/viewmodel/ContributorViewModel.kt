package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContributorViewModel(private val reVancedAPI: ReVancedAPI) : ViewModel() {
    val repositories = mutableStateListOf<ReVancedGitRepository>()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { reVancedAPI.getContributors().getOrNull() }?.let(
                repositories::addAll
            )
        }
    }
}