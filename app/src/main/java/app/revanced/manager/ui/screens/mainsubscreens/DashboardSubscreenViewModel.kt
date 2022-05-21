package app.revanced.manager.ui.screens.mainsubscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.Global
import app.revanced.manager.backend.api.GitHubAPI
import kotlinx.coroutines.launch

class DashboardSubscreenViewModel() : ViewModel() {
    var aboba : String = ""


    init {
        viewModelScope.launch {
            aboba = GitHubAPI.Commits.latestCommit(Global.ghPatcher, "HEAD").commitObj.author.date
        }
    }
}