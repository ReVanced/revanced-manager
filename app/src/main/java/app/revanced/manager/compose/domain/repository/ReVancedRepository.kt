package app.revanced.manager.compose.domain.repository

import app.revanced.manager.compose.network.api.PatchesAsset
import app.revanced.manager.compose.network.dto.ReVancedReleases
import app.revanced.manager.compose.network.dto.ReVancedRepositories
import app.revanced.manager.compose.network.service.ReVancedService
import app.revanced.manager.compose.network.utils.APIResponse

interface ReVancedRepository {
    suspend fun getAssets(): APIResponse<ReVancedReleases>

    suspend fun getContributors(): APIResponse<ReVancedRepositories>

    suspend fun findAsset(repo: String, file: String): PatchesAsset
}

class ReVancedRepositoryImpl(
    private val service: ReVancedService
) : ReVancedRepository {
    override suspend fun getAssets() = service.getAssets()

    override suspend fun getContributors() = service.getContributors()

    override suspend fun findAsset(repo: String, file: String) = service.findAsset(repo, file)
}