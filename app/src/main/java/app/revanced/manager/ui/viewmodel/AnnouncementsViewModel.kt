package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.network.dto.ReVancedAnnouncement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val network: NetworkInfo,
    val preferences: PreferencesManager
) : ViewModel() {

    private var allAnnouncements by mutableStateOf<List<ReVancedAnnouncement>?>(null)

    var announcements by mutableStateOf<List<ReVancedAnnouncement>?>(null)
        private set

    var tags by mutableStateOf<List<String>?>(null)
        private set

    val selectedTags = mutableStateListOf<String>()

    init {
        loadData()
        observeSelectedTags()
    }

    fun markAnnouncementRead(id: Long) {
        viewModelScope.launch {
            preferences.edit {
                preferences.readAnnouncements += id.toString()
            }
        }
    }

    private fun saveSelectedTags() {
        viewModelScope.launch {
            preferences.selectedAnnouncementTags.update(selectedTags.toSet())
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            if (!network.isConnected()) return@launch

            val savedTags = preferences.selectedAnnouncementTags.get()
            selectedTags.apply {
                clear()
                addAll(savedTags)
            }

            withContext(Dispatchers.IO) {
                announcementRepository.getAnnouncements()?.let {
                    allAnnouncements = it
                }
                announcementRepository.getTags()?.let {
                    tags = it.map { tag -> tag.name }
                }
            }

            applyTagFilter()
        }
    }

    private fun applyTagFilter() {
        val selected = selectedTags.toList()
        announcements = if (selected.isEmpty()) {
            allAnnouncements
        } else {
            allAnnouncements?.filter { announcement ->
                announcement.tags.any { it in selected }
            }
        }
    }

    private fun observeSelectedTags() {
        viewModelScope.launch {
            snapshotFlow { selectedTags.toList() }.collect { _ ->
                saveSelectedTags()
                applyTagFilter()
            }
        }
    }
}