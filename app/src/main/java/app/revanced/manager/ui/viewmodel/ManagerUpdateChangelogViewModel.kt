package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class ManagerUpdateChangelogViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
) : ViewModel() {
    var changelog by mutableStateOf(
        Changelog(
            "...",
            app.getString(R.string.changelog_loading),
        )
    )
    val markdown by derivedStateOf {
        changelog.body
    }

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                changelog = api.getRelease("revanced-manager").getOrThrow().let {
                    Changelog(it.metadata.tag, it.metadata.body)
                }
            }
        }
    }

    data class Changelog(
        val version: String,
        val body: String,
    )
}
