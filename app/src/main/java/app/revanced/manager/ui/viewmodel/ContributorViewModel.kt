package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContributorViewModel(private val reVancedAPI: ReVancedAPI) : ViewModel() {
    var repositories: List<ReVancedGitRepository>? by mutableStateOf(null)
    	private set

    init {
        viewModelScope.launch {
            repositories = withContext(Dispatchers.IO) {
                reVancedAPI.getContributors().getOrNull()
            }
        }
    }
}