package app.revanced.manager.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.backend.api.GitHubAPI
import kotlinx.coroutines.launch

class ContributorsViewModel : ViewModel() {
    private val tag = "ContributorsViewModel"

    val contributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()

    fun load() {
        viewModelScope.launch {
            val githubContributors = GitHubAPI.Contributors.contributors("Aunali321","revanced-manager")
            githubContributors.sortedByDescending {
                it.login
            }
            contributorsList.addAll(githubContributors)
        }
    }

    init {
        load()
    }

}