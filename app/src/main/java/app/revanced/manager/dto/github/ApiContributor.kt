package app.revanced.manager.dto.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ApiContributor(
    @SerialName("login") val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val profileUrl: String,
)