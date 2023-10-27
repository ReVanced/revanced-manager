package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
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
    var changelogs = mutableStateListOf(
        Changelog(
            "...",
            app.getString(R.string.changelog_loading),
        )
    )

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                api.getReleases("revanced-manager").getOrThrow().let { releases ->
                    changelogs.clear()
                    changelogs.addAll(releases.map { Changelog(it.metadata.tag, it.metadata.body) })
                }
            }
        }
    }

    data class Changelog(
        val version: String,
        val body: String,
    )
}