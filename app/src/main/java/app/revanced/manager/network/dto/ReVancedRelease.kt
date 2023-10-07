package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReVancedLatestRelease(
    val release: ReVancedRelease,
)

@Serializable
data class ReVancedRelease(
    val metadata: ReVancedReleaseMeta,
    val assets: List<Asset>
)

@Serializable
data class ReVancedReleaseMeta(
    @SerialName("tag_name") val tag: String,
    val name: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("published_at") val publishedAt: String,
    val body: String,
)

@Serializable
data class Asset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String
)