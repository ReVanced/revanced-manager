package app.revanced.manager.compose.network.service

import app.revanced.manager.compose.network.api.MissingAssetException
import app.revanced.manager.compose.network.api.PatchesAsset
import app.revanced.manager.compose.network.dto.ReVancedReleases
import app.revanced.manager.compose.network.dto.ReVancedRepositories
import app.revanced.manager.compose.network.utils.APIResponse
import app.revanced.manager.compose.network.utils.getOrNull
import app.revanced.manager.compose.util.apiURL
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ReVancedService {
    suspend fun getAssets(): APIResponse<ReVancedReleases>

    suspend fun getContributors(): APIResponse<ReVancedRepositories>

    suspend fun findAsset(repo: String, file: String): PatchesAsset
}

class ReVancedServiceImpl(
    private val client: HttpService,
) : ReVancedService {
    override suspend fun getAssets(): APIResponse<ReVancedReleases> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$apiUrl/tools")
            }
        }
    }

    override suspend fun getContributors(): APIResponse<ReVancedRepositories> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$apiUrl/contributors")
            }
        }
    }

    override suspend fun findAsset(repo: String, file: String): PatchesAsset {
        val releases = getAssets().getOrNull() ?: throw Exception("Cannot retrieve assets")
        val asset = releases.tools.find { asset ->
            (asset.name.contains(file) && asset.repository.contains(repo))
        } ?: throw MissingAssetException()
        return PatchesAsset(asset.downloadUrl, asset.name)
    }

    private companion object {
        private const val apiUrl = apiURL
    }
}