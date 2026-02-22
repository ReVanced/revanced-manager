package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.network.dto.ReVancedAnnouncement
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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
    val showArchived = MutableStateFlow(false)

    val announcements = combine(
        allAnnouncements,
        _tags,
        selectedTags.flow,
        showArchived
    ) { source, tags, selectedTags, showArchived ->
        if (source == null) return@combine null
        // Only filter by tags that actually
        val availableTags = tags.orEmpty().toSet()
        val validSelected = selectedTags.intersect(availableTags)

        val visibleAnnouncements = if (showArchived) {
            source
        } else {
            source.filter { announcement ->
                announcement.archivedAt.toInstant(TimeZone.UTC) > Clock.System.now()
            }
        }

        if (validSelected.isEmpty()) {
            visibleAnnouncements
        } else {
            visibleAnnouncements.filter { announcement ->
                announcement.tags.any(validSelected::contains)
            }
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
            if (!network.isConnected()) return@launch

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