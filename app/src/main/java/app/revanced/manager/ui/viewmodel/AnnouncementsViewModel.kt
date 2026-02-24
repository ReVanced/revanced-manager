package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.network.dto.ReVancedAnnouncement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    private val network: NetworkInfo,
    private val preferences: PreferencesManager
) : ViewModel() {
    private val allAnnouncements = MutableStateFlow<List<ReVancedAnnouncement>?>(null)

    private val _tags = MutableStateFlow<List<String>?>(null)
    val tags get() = _tags.asStateFlow()
    val selectedTags = preferences.selectedAnnouncementTags
    val readAnnouncements = preferences.readAnnouncements

    val announcements = combine(
        allAnnouncements,
        _tags,
        selectedTags.flow
    ) { source, tags, selectedTags ->
        if (source == null) return@combine null
        // Only filter by tags that actually exist
        val availableTags = tags.orEmpty().toSet()
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
                announcement.archivedAt.toInstant(TimeZone.UTC) > now
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
            if (!network.isConnected()) {
                allAnnouncements.value = emptyList()
                _tags.value = emptyList()
                return@launch
            }

            withContext(Dispatchers.IO) {
                announcementRepository.getAnnouncements()?.let {
                    allAnnouncements.value = it
                }
                announcementRepository.getTags()?.let {
                    _tags.value = it.map { tag -> tag.name }
                }
            }
        }
    }
}