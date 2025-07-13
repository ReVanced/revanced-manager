package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReVancedGitRepository(
    val name: String,
    val url: String,
    val contributors: List<ReVancedContributor>,
)

@Serializable
data class ReVancedContributor(
    @SerialName("name") val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
)