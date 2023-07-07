package app.revanced.manager.network.service

import app.revanced.manager.network.dto.GithubChangelog
import app.revanced.manager.network.utils.APIResponse
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubService(private val client: HttpService) {
    suspend fun getChangelog(repo: String): APIResponse<GithubChangelog> = withContext(Dispatchers.IO) {
        client.request {
            url("https://api.github.com/repos/revanced/$repo/releases/latest")
        }
    }
}