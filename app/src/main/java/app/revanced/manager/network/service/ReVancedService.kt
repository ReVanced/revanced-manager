package app.revanced.manager.network.service

import app.revanced.manager.network.dto.ReVancedLatestRelease
import app.revanced.manager.network.dto.ReVancedGitRepositories
import app.revanced.manager.network.utils.APIResponse
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReVancedService(
    private val client: HttpService,
) {
    suspend fun getRelease(api: String, repo: String): APIResponse<ReVancedLatestRelease> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$api/v2/$repo/releases/latest")
            }
        }

    suspend fun getContributors(api: String): APIResponse<ReVancedGitRepositories> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$api/contributors")
            }
        }
}