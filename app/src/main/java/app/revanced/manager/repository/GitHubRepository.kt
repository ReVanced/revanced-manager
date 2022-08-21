package app.revanced.manager.repository

import app.revanced.manager.dto.github.ApiCommit
import app.revanced.manager.dto.github.ApiContributor
import app.revanced.manager.dto.github.ApiRelease
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubRepository(private val client: HttpClient) {
    suspend fun getLatestRelease(repo: String) = withContext(Dispatchers.IO) {
        val res: List<ApiRelease> = client.get("$baseUrl/$repo/releases") {
            parameter("per_page", 1)
        }.body()
        res.first()
    }

    suspend fun getLatestCommit(repo: String, ref: String) = withContext(Dispatchers.IO) {
        client.get("$baseUrl/$repo/commits/$ref") {
            parameter("per_page", 1)
        }.body() as ApiCommit
    }

    suspend fun getContributors(org: String, repo: String) = withContext(Dispatchers.IO) {
        client.get("$baseUrl/$org/$repo/contributors").body() as List<ApiContributor>
    }

    private companion object {
        private const val baseUrl = "https://api.github.com/repos"
    }
}