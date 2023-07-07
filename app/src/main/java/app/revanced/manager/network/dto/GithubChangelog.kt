package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubChangelog(
    @SerialName("tag_name") val version: String,
    @SerialName("body") val body: String,
    @SerialName("assets") val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    @SerialName("download_count") val downloadCount: Int,
)