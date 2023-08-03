package app.revanced.manager.network.service

import app.revanced.manager.network.api.MissingAssetException
import app.revanced.manager.network.dto.Asset
import app.revanced.manager.network.dto.ReVancedReleases
import app.revanced.manager.network.dto.ReVancedRepositories
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReVancedService(
    private val client: HttpService,
) {
    suspend fun getAssets(api: String): APIResponse<ReVancedReleases> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$api/tools")
            }
        }
    }

    suspend fun getContributors(api: String): APIResponse<ReVancedRepositories> {
        return withContext(Dispatchers.IO) {
            client.request {
                url("$api/contributors")
            }
        }
    }

}