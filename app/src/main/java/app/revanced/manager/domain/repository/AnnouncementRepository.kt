package app.revanced.manager.domain.repository

import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnnouncementRepository(
    private val api: ReVancedAPI
) {
    private val mutex = Mutex()
    private var cachedAnnouncements: List<ReVancedAnnouncement>? = null

    suspend fun getAnnouncements(forceRefresh: Boolean = false): List<ReVancedAnnouncement>? {
        mutex.withLock {
            if (cachedAnnouncements == null || forceRefresh) {
                cachedAnnouncements = api.getAnnouncements().getOrNull()
            }
            return cachedAnnouncements
        }
    }
}
