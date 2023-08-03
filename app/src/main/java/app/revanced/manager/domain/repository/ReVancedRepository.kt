package app.revanced.manager.domain.repository

import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.api.MissingAssetException
import app.revanced.manager.network.dto.Asset
import app.revanced.manager.network.dto.ReVancedReleases
import app.revanced.manager.network.service.ReVancedService
import app.revanced.manager.network.utils.getOrThrow

class ReVancedRepository(
    private val service: ReVancedService,
    private val prefs: PreferencesManager
) {
    private suspend fun apiUrl() = prefs.api.get()

    suspend fun getContributors() = service.getContributors(apiUrl())

    suspend fun getAssets() = Assets(service.getAssets(apiUrl()).getOrThrow())
}

class Assets(private val releases: ReVancedReleases): List<Asset> by releases.tools {
    fun find(repo: String, file: String) = find { asset ->
        asset.name.contains(file) && asset.repository.contains(repo)
    } ?: throw MissingAssetException()
}