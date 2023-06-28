package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.ReVancedRepository
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContributorViewModel(private val repository: ReVancedRepository): ViewModel() {
    val repositories = mutableStateListOf<app.revanced.manager.network.dto.ReVancedRepository>()
    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val repos = repository.getContributors().getOrNull()?.repositories
                withContext(Dispatchers.Main) {
                    if (repos != null) { repositories.addAll(repos) }
                }
            }
        }
    }
}