package app.revanced.manager.network.api

import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.dto.Asset
import app.revanced.manager.network.dto.ReVancedLatestRelease
import app.revanced.manager.network.dto.ReVancedRelease
import app.revanced.manager.network.service.ReVancedService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.network.utils.transform

class ReVancedAPI(
    private val service: ReVancedService,
    private val prefs: PreferencesManager
) {
    private suspend fun apiUrl() = prefs.api.get()

    suspend fun getContributors() = service.getContributors(apiUrl()).transform { it.repositories }

    suspend fun getRelease(name: String) = service.getRelease(apiUrl(), name).transform { it.release }

    companion object Extensions {
        fun ReVancedRelease.findAssetByType(mime: String) = assets.singleOrNull { it.contentType == mime } ?: throw MissingAssetException(mime)
    }
}

class MissingAssetException(type: String) : Exception("No asset with type $type")