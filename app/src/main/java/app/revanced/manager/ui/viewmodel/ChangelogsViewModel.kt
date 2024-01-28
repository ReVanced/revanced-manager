package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.api.ReVancedAPI.Extensions.findAssetByType
import app.revanced.manager.network.utils.getOrNull
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class ChangelogsViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
) : ViewModel() {
    var changelogs: List<Changelog>? by mutableStateOf(null)

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                changelogs = api.getReleases("revanced-manager").getOrNull().orEmpty().map { release ->
                    Changelog(
                        release.version,
                        release.findAssetByType(APK_MIMETYPE).downloadCount,
                        release.metadata.publishedAt,
                        release.metadata.body
                    )
                }
            }
        }
    }

    data class Changelog(
        val version: String,
        val downloadCount: Int,
        val publishDate: String,
        val body: String,
    )
}