package app.revanced.manager.domain.repository

import app.revanced.manager.network.api.ReVancedAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ManagerUpdateRepository(
    private val reVancedAPI: ReVancedAPI
) {
    private val _availableVersion = MutableStateFlow<String?>(null)
    val availableVersion = _availableVersion.asStateFlow()

    suspend fun refreshAvailableVersion(): String? {
        val version = reVancedAPI.getAppUpdate()?.version
        _availableVersion.value = version
        return version
    }

    fun clearAvailableVersion() {
        _availableVersion.value = null
    }
}