package app.revanced.manager.network.service

import app.revanced.manager.network.api.MissingAssetException
import app.revanced.manager.network.dto.Asset
import app.revanced.manager.network.dto.ReVancedReleases
import app.revanced.manager.network.dto.ReVancedRepositories
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.apiURL
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReVancedService(
    private val client: HttpService,
) {
    suspend fun getAssets(): APIResponse<ReVancedReleases> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$apiUrl/tools")
            }
        }
    }

    suspend fun getContributors(): APIResponse<ReVancedRepositories> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$apiUrl/contributors")
            }
        }
    }

    suspend fun findAsset(repo: String, file: String): Asset {
        val releases = getAssets().getOrThrow()

        val asset = releases.tools.find { asset ->
            (asset.name.contains(file) && asset.repository.contains(repo))
        } ?: throw MissingAssetException()

        return asset
    }

    private companion object {
        private const val apiUrl = apiURL
    }
}