package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ReVancedRepositories(
    @SerialName("repositories") val repositories: List<ReVancedRepository>,
)

@Serializable
class ReVancedRepository(
    @SerialName("name") val name: String,
    @SerialName("contributors") val contributors: List<ReVancedContributor>,
)

@Serializable
class ReVancedContributor(
    @SerialName("login") val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
)
