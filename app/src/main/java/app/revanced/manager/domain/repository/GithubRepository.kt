package app.revanced.manager.domain.repository

import app.revanced.manager.network.service.GithubService

// TODO: delete this when the revanced api adds download count.
class GithubRepository(private val service: GithubService) {
    suspend fun getChangelog(repo: String) = service.getChangelog(repo)
}