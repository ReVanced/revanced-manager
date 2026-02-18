package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementViewModel(
    announcementId: Long,
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo
): ViewModel() {

    var announcement by mutableStateOf<ReVancedAnnouncement?>(null)
        private set

    val isConnected: Boolean
        get() = network.isConnected()

    init {
        viewModelScope.launch {
            if (!isConnected) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                reVancedAPI.getAnnouncement(announcementId).getOrNull()
            }?.let {
                announcement = it
            }
        }
    }

}