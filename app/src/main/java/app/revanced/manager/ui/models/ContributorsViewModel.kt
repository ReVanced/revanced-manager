package app.revanced.manager.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.backend.api.GitHubAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContributorsViewModel : ViewModel() {
    private val tag = "ContributorsViewModel"

    val patcherContributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()
    val patchesContributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()
    val cliContributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()
    val managerContributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()
    val integrationsContributorsList = mutableStateListOf<GitHubAPI.Contributors.Contributor>()

    suspend fun loadContributors() {
        withContext(Dispatchers.IO) {
            viewModelScope.launch {
                val cliContributors =
                    GitHubAPI.Contributors.contributors("revanced", "revanced-cli")
                cliContributors.sortedByDescending {
                    it.login
                }
                cliContributorsList.addAll(cliContributors)

                val patcherContributors =
                    GitHubAPI.Contributors.contributors("revanced", "revanced-patcher")
                patcherContributors.sortedByDescending {
                    it.login
                }
                patcherContributorsList.addAll(patcherContributors)

                val patchesContributors =
                    GitHubAPI.Contributors.contributors("revanced", "revanced-patches")
                patchesContributors.sortedByDescending {
                    it.login
                }
                patchesContributorsList.addAll(patchesContributors)

                val managerContributors =
                    GitHubAPI.Contributors.contributors("Aunali321", "revanced-manager")
                managerContributors.sortedByDescending {
                    it.login
                }
                managerContributorsList.addAll(managerContributors)

                val integrationsContributors =
                    GitHubAPI.Contributors.contributors("revanced", "revanced-integrations")
                integrationsContributors.sortedByDescending {
                    it.login
                }
                integrationsContributorsList.addAll(integrationsContributors)
            }

        }
    }

    init {
        GlobalScope.launch {
            loadContributors()
        }
    }

}