package app.revanced.manager.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReVancedSocials(
    val socials: List<ReVancedSocial>,
)

@Serializable
data class ReVancedSocial(
    val name: String,
    val url: String,
)
