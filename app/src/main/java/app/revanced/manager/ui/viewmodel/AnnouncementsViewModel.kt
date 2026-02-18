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
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plus

class AnnouncementsViewModel(
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo,
    private val preferences: PreferencesManager
): ViewModel() {

    private var _announcements: List<ReVancedAnnouncement>? = null

    var announcements by mutableStateOf<List<ReVancedAnnouncement>?>(null)
        private set

    var tags by mutableStateOf<List<String>?>(null)
        private set

    val readAnnouncements = mutableStateListOf<Long>()

    val selectedTags = mutableStateListOf<String>()

    val isConnected: Boolean
        get() = network.isConnected()

    init {
        fetchAnnouncements()
        fetchTags()
        filterTags()
        observeUnreads()
    }

    fun markUnreadAnnouncementRead(id: Long) {
        readAnnouncements.add(id)
        viewModelScope.launch {
            val readAnnouncements = preferences.readAnnouncements.get()
            preferences.readAnnouncements.update(readAnnouncements + id.toString())
        }
    }

    private fun fetchAnnouncements() {
        viewModelScope.launch {
            if (!isConnected) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                reVancedAPI.getAnnouncements().getOrNull()
            }?.let {
                _announcements = it
                selectedTags.addAll(listOf("✨ ReVanced", "manager"))
            }
        }
    }

    private fun fetchTags() {
        viewModelScope.launch {
            if (!isConnected) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                reVancedAPI.getAnnouncementTags().getOrNull()
            }?.let {
                tags = it.map { it.name }
            }
        }
    }

    private fun filterTags() {
        viewModelScope.launch {
            snapshotFlow { selectedTags.toList() }.collect {
                announcements = _announcements?.filter { announcement ->
                    selectedTags.any { selectedTag ->
                        announcement.tags.contains(selectedTag)
                    }
                }
            }
        }
    }

    private fun observeUnreads() {
        viewModelScope.launch {
            preferences.readAnnouncements.flow.collect { announcements ->
                readAnnouncements.clear()
                announcements.forEach {
                    readAnnouncements.add(it.toLong())
                }
            }
        }
    }

}