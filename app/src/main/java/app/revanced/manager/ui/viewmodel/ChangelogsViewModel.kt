package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class ChangelogsViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
) : ViewModel() {
    var releaseInfo: ReVancedAsset? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                releaseInfo = api.getLatestAppInfo().getOrThrow()
            }
        }
    }
}