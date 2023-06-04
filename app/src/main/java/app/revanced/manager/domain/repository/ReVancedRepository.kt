package app.revanced.manager.domain.repository

import app.revanced.manager.network.service.ReVancedService

class ReVancedRepository(
    private val service: ReVancedService
) {
    suspend fun getAssets() = service.getAssets()

    suspend fun getContributors() = service.getContributors()

    suspend fun findAsset(repo: String, file: String) = service.findAsset(repo, file)
}