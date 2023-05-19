package app.revanced.manager.compose.domain.repository

import app.revanced.manager.compose.network.service.ReVancedService

class ReVancedRepository(
    private val service: ReVancedService
) {
    suspend fun getAssets() = service.getAssets()

    suspend fun getContributors() = service.getContributors()

    suspend fun findAsset(repo: String, file: String) = service.findAsset(repo, file)
}