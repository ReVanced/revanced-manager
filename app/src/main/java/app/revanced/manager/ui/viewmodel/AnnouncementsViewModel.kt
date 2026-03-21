package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.network.dto.ReVancedAnnouncement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

data class AnnouncementSections(
    val activeAnnouncements: List<ReVancedAnnouncement>,
    val archivedAnnouncements: List<ReVancedAnnouncement>
) {
    val isEmpty: Boolean
        get() = activeAnnouncements.isEmpty() && archivedAnnouncements.isEmpty()
}

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val preferences: PreferencesManager
) : ViewModel() {
    private val allAnnouncements = MutableStateFlow<List<ReVancedAnnouncement>?>(null)

    val tags = allAnnouncements.map { it?.tags }
    val selectedTags = preferences.selectedAnnouncementTags
    val readAnnouncements = preferences.readAnnouncements

    val announcements = combine(
        allAnnouncements,
        selectedTags.flow
    ) { source, selectedTags ->
        if (source == null) return@combine null
        // Only filter by tags that actually exist
        val availableTags = source.tags
        val validSelected = selectedTags.intersect(availableTags)

        if (validSelected.isEmpty()) {
            source
        } else {
            source.filter { announcement ->
                announcement.tags.any(validSelected::contains)
            }
        }
    }

    val announcementSections = announcements.map { announcementList ->
        announcementList?.let { announcements ->
            val now = Clock.System.now()
            val (activeAnnouncements, archivedAnnouncements) = announcements.partition { announcement ->
                announcement.archivedAt ?: return@partition true
                announcement.archivedAt > now
            }
            AnnouncementSections(
                activeAnnouncements = activeAnnouncements,
                archivedAnnouncements = archivedAnnouncements
            )
        }
    }

    init {
        loadData()
    }

    fun markAnnouncementRead(id: Long) {
        viewModelScope.launch {
            preferences.edit {
                preferences.readAnnouncements += id
            }
        }
    }

    fun changeTagSelection(tag: String) = viewModelScope.launch {
        preferences.edit {
            if (tag in selectedTags.value) selectedTags -= tag
            else selectedTags += tag
        }
    }

    fun resetTagSelection() = viewModelScope.launch {
        selectedTags.update(preferences.selectedAnnouncementTags.default)
    }

    private fun loadData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                announcementRepository.getAnnouncements()?.let {
                    allAnnouncements.value = it
                }
            }
        }
    }

    private companion object {
        val List<ReVancedAnnouncement>.tags: Set<String>
            get() = flatMapTo(
                mutableSetOf()
            ) { it.tags }
    }
}