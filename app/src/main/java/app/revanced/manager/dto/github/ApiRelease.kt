package app.revanced.manager.dto.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("published_at") val publishedAt: String,
    val prerelease: Boolean,
    val assets: List<Asset>,
    val body: String
) {
    @Serializable
    data class Asset(
        @SerialName("browser_download_url") val downloadUrl: String,
        val name: String
    )
}