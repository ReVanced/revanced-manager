package app.revanced.manager.ui.models

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.backend.api.GitHubAPI
import kotlinx.coroutines.launch

class AboutViewModel : ViewModel() {
    private val tag = "AboutViewModel"

    private var _fetchContributorName : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
    val contributorName: String
        get() = _fetchContributorName?.login ?: "Null"

    private var _fetchContributorAvatar : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
    val contributorAvatar: String
        get() = _fetchContributorAvatar?.avatar_url ?: "Null"

    private var _fetchContributorProfile : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
    val contributorProfile: String
        get() = _fetchContributorProfile?.url ?: "Null"

    init {
        fetchContributors()
    }
    private fun fetchContributors() {
        viewModelScope.launch {
            try {
                _fetchContributorName = GitHubAPI.Contributors.contributors("revanced", "revanced-manager").elementAt(4)
            } catch (e: Exception) {
                Log.e(tag, "failed to fetch contributor names", e)
            }
            try {
                _fetchContributorAvatar = GitHubAPI.Contributors.contributors("revanced", "revanced-manager").elementAt(4)
            } catch (e: Exception) {
                Log.e(tag, "failed to fetch latest contributor avatar", e)
            }
        }
    }
}