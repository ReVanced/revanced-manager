package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReVancedGitRepositories(
    val repositories: List<ReVancedGitRepository>,
)

@Serializable
data class ReVancedGitRepository(
    val name: String,
    val contributors: List<ReVancedContributor>,
)

@Serializable
data class ReVancedContributor(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
)
