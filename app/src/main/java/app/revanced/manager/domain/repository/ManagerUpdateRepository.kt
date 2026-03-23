package app.revanced.manager.domain.repository

import app.revanced.manager.BuildConfig
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.utils.getOrThrow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime

class ManagerUpdateRepository(
    private val reVancedAPI: ReVancedAPI
) {
    private val asset: ReVancedAsset? = null
    private val _releasedAt = MutableStateFlow<LocalDateTime?>(null)
    private val _version = MutableStateFlow<String?>(null)
    private val _hasUpdate = MutableStateFlow(false)

    val releasedAt = _releasedAt.asStateFlow()
    val hasUpdate = _hasUpdate.asStateFlow()
    val version = _version.asStateFlow()

    suspend fun refresh(): ReVancedAsset {
        val update = reVancedAPI.getLatestAppInfo().getOrThrow()

        _releasedAt.value = update.createdAt
        _version.value = update.version
        _hasUpdate.value = update.version.removePrefix("v") != BuildConfig.VERSION_NAME

        return update
    }

    suspend fun getUpdateOrNull(refetch: Boolean = false): ReVancedAsset? {
        val asset = if (refetch || asset == null) refresh() else null
        return asset.takeIf { _hasUpdate.value }
    }

    fun clearState() {
        _releasedAt.value = null
        _version.value = null
        _hasUpdate.value = false
    }
}