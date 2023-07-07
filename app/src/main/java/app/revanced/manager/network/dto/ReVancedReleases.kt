package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ReVancedReleases(
    @SerialName("tools") val tools: List<Asset>,
)

@Serializable
class Asset(
    @SerialName("repository") val repository: String,
    @SerialName("version") val version: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: String?,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val content_type: String
)