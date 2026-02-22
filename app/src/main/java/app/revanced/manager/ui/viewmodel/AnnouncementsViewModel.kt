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
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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

    var showArchived by mutableStateOf(false)

    private var savedTags = emptySet<String>()

    init {
        loadData()
        observeSelectedTags()
    }

    fun markAnnouncementRead(id: Long) {
        viewModelScope.launch {
            preferences.edit {
                preferences.readAnnouncements += id
            }
        }
    }

    fun saveSelectedTags() {
        viewModelScope.launch {
            preferences.selectedAnnouncementTags.update(selectedTags.toSet())
            savedTags = selectedTags.toSet()
        }
    }

    fun resetTagSelection() {
        selectedTags.clear()
        selectedTags.addAll(preferences.selectedAnnouncementTags.default)
    }

    private fun loadData() {
        viewModelScope.launch {
            if (!network.isConnected()) return@launch

            withContext(Dispatchers.IO) {
                announcementRepository.getAnnouncements()?.let {
                    allAnnouncements = it
                }
                announcementRepository.getTags()?.let {
                    tags = it.map { tag -> tag.name }
                }
            }

            savedTags = preferences.selectedAnnouncementTags.get()
            selectedTags.apply {
                clear()
                addAll(savedTags)
            }

            applyTagFilter()
        }
    }

    private fun applyTagFilter() {
        val source = allAnnouncements ?: return
        val selected = selectedTags.toSet()
        val availableTags = tags.orEmpty().toSet()
        // Only filter by tags that actually exist
        val validSelected = selected.intersect(availableTags)

        val visibleAnnouncements = if (showArchived) {
            source
        } else {
            source.filter { announcement ->
                announcement.archivedAt.toInstant(TimeZone.UTC) > Clock.System.now()
            }
        }

        announcements = if (validSelected.isEmpty()) {
            visibleAnnouncements
        } else {
            visibleAnnouncements.filter { announcement ->
                announcement.tags.any(validSelected::contains)
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