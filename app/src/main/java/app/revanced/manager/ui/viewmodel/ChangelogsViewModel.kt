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
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json

sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Success(val changelogs: List<ReVancedAsset>) : ChangelogUiState
    data class Error(val message: String) : ChangelogUiState
}

class ChangelogsViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
) : ViewModel() {
    val mock = listOf(
        ReVancedAsset(
            version = "v1.25.2",
            createdAt = LocalDateTime.parse("2025-07-07T13:01:12"),
            description = """
            ## [1.25.2](https://github.com/ReVanced/revanced-manager/compare/v1.25.0...v1.25.1) (2025-07-07)

            ### Bug Fixes

            * Disable Impeller Flutter render engine correctly to fix rendering issues
        """.trimIndent(),
            downloadUrl = "https://github.com/ReVanced/revanced-manager/releases/download/v1.25.1/revanced-manager-1.25.1.apk"
        ),
        ReVancedAsset(
            version = "v1.25.1",
            createdAt = LocalDateTime.parse("2025-07-07T13:01:12"),
            description = """
            ## [1.25.1](https://github.com/ReVanced/revanced-manager/compare/v1.25.0...v1.25.1) (2025-07-07)

            ### Bug Fixes

            * Disable Impeller Flutter render engine correctly to fix rendering issues
        """.trimIndent(),
            downloadUrl = "https://github.com/ReVanced/revanced-manager/releases/download/v1.25.1/revanced-manager-1.25.1.apk"
        )
    )

    var state: ChangelogUiState by mutableStateOf(ChangelogUiState.Loading)
        private set

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                api.getLatestAppInfo().getOrThrow()
                state = ChangelogUiState.Success(changelogs = mock)
            }
            if (state is ChangelogUiState.Loading) {
                state = ChangelogUiState.Error(app.getString(R.string.changelog_download_fail))
            }
        }
    }
}